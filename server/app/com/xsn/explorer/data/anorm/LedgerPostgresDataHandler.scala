package com.xsn.explorer.data.anorm

import java.sql.Connection

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ApplicationError
import com.xsn.explorer.data.LedgerBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.{AggregatedAmountPostgresDAO, BalancePostgresDAO, BlockPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.errors.{PostgresForeignKeyViolationError, PreviousBlockMissingError, RepeatedBlockHeightError}
import com.xsn.explorer.models.persisted.{Balance, Block, Transaction}
import com.xsn.explorer.models.Address
import com.xsn.explorer.util.Extensions.ListOptionExt
import javax.inject.Inject
import org.scalactic.Good
import org.slf4j.LoggerFactory
import play.api.db.Database

class LedgerPostgresDataHandler @Inject() (
    override val database: Database,
    blockPostgresDAO: BlockPostgresDAO,
    transactionPostgresDAO: TransactionPostgresDAO,
    balancePostgresDAO: BalancePostgresDAO,
    aggregatedAmountPostgresDAO: AggregatedAmountPostgresDAO)
    extends LedgerBlockingDataHandler
    with AnormPostgresDataHandler {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Push a block into the database chain, note that even if the block is supposed
   * to have a next block, we remove the link because that block is not stored yet.
   */
  override def push(
      block: Block,
      transactions: List[Transaction]): ApplicationResult[Unit] = {

    val result = withTransaction { implicit conn =>
      val result = for {
        _ <- upsertBlockCascade(block.copy(nextBlockhash = None), transactions)
      } yield ()

      result
          .map(Good(_))
          .getOrElse(throw new RuntimeException("Unable to push block"))
    }

    def fromError(e: ApplicationError) = e match {
      case PostgresForeignKeyViolationError("previous_blockhash", _) => PreviousBlockMissingError
      case PostgresForeignKeyViolationError("height", _) => RepeatedBlockHeightError
      case _ => e
    }

    result.badMap { _.map(fromError) }
  }

  /**
   * Pop a block from the database chain (if exists)
   */
  override def pop(): ApplicationResult[Block] = withTransaction { implicit conn =>
    val result = for {
      block <- blockPostgresDAO.getLatestBlock
      _ <- deleteBlockCascade(block)
    } yield block

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to pop block"))
  }

  private def upsertBlockCascade(block: Block, transactions: List[Transaction])(implicit conn: Connection): Option[Unit] = {
    val result = for {
      // block
      _ <- deleteBlockCascade(block).orElse(Some(()))
      _ <- blockPostgresDAO.insert(block)

      // batch insert
      _ <- transactionPostgresDAO.insert(transactions)

      // balances
      balanceList = balances(transactions)
      _ <- insertBalanceBatch(balanceList).toList.everything

      // compute aggregated amount
      delta = balanceList.map(_.available).sum
      _ = aggregatedAmountPostgresDAO.updateAvailableCoins(delta)
    } yield ()

    // link previous block (if possible)
    block.previousBlockhash.foreach { previousBlockhash =>
      blockPostgresDAO
          .setNextBlockhash(previousBlockhash, block.hash)
    }

    result
  }

  private def deleteBlockCascade(block: Block)(implicit conn: Connection): Option[Unit] = {
    // transactions
    val deletedTransactions = transactionPostgresDAO.deleteBy(block.hash)
    for {
      // block
      _ <- blockPostgresDAO.delete(block.hash)

      // balances
      balanceList = balances(deletedTransactions)
      _ <- balanceList
          .map { b => b.copy(spent = -b.spent, received = -b.received) }
          .map { b => balancePostgresDAO.upsert(b) }
          .toList
          .everything

      // compute aggregated amount
      delta = balanceList.map(_.available).sum
      _ = aggregatedAmountPostgresDAO.updateAvailableCoins(-delta)
    } yield ()
  }

  private def insertBalanceBatch(balanceList: Iterable[Balance])(implicit conn: Connection) = {
    balanceList.map { b => balancePostgresDAO.upsert(b) }
  }

  private def spendMap(transactions: List[Transaction]): Map[Address, BigDecimal] = {
    transactions
        .map(_.inputs)
        .flatMap { inputs =>
          inputs.map { input => input.address -> input.value }
        }
        .groupBy(_._1)
        .mapValues { list => list.map(_._2).sum }
  }

  private def receiveMap(transactions: List[Transaction]): Map[Address, BigDecimal] = {
    transactions
        .map(_.outputs)
        .flatMap { outputs =>
          outputs.map { output =>
            output.address -> output.value
          }
        }
        .groupBy(_._1)
        .mapValues { list => list.map(_._2).sum }
  }

  private def balances(transactions: List[Transaction]) = {
    val spentList = spendMap(transactions).map { case (address, spent) =>
      Balance(address, spent = spent)
    }

    val receiveList = receiveMap(transactions).map { case (address, received) =>
      Balance(address, received = received)
    }

    val result = (spentList ++ receiveList)
        .groupBy(_.address)
        .mapValues { _.reduce(mergeBalances) }
        .values

    result
  }

  private def mergeBalances(a: Balance, b: Balance): Balance = {
    Balance(a.address, spent = a.spent + b.spent, received = a.received + b.received)
  }
}
