package com.xsn.explorer.data.anorm

import java.sql.Connection
import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.DatabaseBlockingSeeder
import com.xsn.explorer.data.DatabaseSeeder._
import com.xsn.explorer.data.anorm.dao.{BalancePostgresDAO, BlockPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Address, Balance, Transaction}
import com.xsn.explorer.util.Extensions.ListOptionExt
import org.scalactic.Good
import org.slf4j.LoggerFactory
import play.api.db.Database

class DatabasePostgresSeeder @Inject() (
    override val database: Database,
    blockPostgresDAO: BlockPostgresDAO,
    transactionPostgresDAO: TransactionPostgresDAO,
    balancePostgresDAO: BalancePostgresDAO)
    extends DatabaseBlockingSeeder
    with AnormPostgresDataHandler {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def newBlock(command: CreateBlockCommand): ApplicationResult[Unit] = withTransaction { implicit conn =>
    val result = for {
      _ <- upsertBlockCascade(command)
    } yield ()

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to add the new block"))
  }

  override def replaceBlock(command: ReplaceBlockCommand): ApplicationResult[Unit] = withTransaction { implicit conn =>
    val createCommand = CreateBlockCommand(command.newBlock, command.newTransactions)

    val result = for {
      _ <- deleteBlockCascade(command.orphanBlock)
      _ <- upsertBlockCascade(createCommand)
    } yield ()

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to replace latest block"))
  }

  private def upsertBlockCascade(command: CreateBlockCommand)(implicit conn: Connection): Option[Unit] = {
    val result = for {
      // block
      _ <- deleteBlockCascade(command.block)
          .orElse(Some(()))

      _ <- blockPostgresDAO.insert(command.block)

      // transactions
      _ <- command.transactions.map(tx => transactionPostgresDAO.upsert(tx)).everything

      // balances
      _ <- balances(command.transactions)
          .map { b => balancePostgresDAO.upsert(b) }
          .toList
          .everything
    } yield ()

    // link previous block (if possible)
    command.block.previousBlockhash.foreach { previousBlockhash =>
      blockPostgresDAO
          .setNextBlockhash(previousBlockhash, command.block.hash)
    }

    result
  }

  private def deleteBlockCascade(block: Block)(implicit conn: Connection): Option[Unit] = {
    for {
      // block
      _ <- blockPostgresDAO.delete(block.hash)

      // transactions
      deletedTransactions = transactionPostgresDAO.deleteBy(block.hash)

      // balances
      _ <- balances(deletedTransactions)
          .map { b => b.copy(spent = -b.spent, received = -b.received) }
          .map { b => balancePostgresDAO.upsert(b) }
          .toList
          .everything
    } yield ()
  }

  private def spendMap(transactions: List[Transaction]): Map[Address, BigDecimal] = {
    transactions
        .map(_.inputs)
        .flatMap { inputs =>
          inputs.flatMap { input =>
            for {
              address <- input.address
              value <- input.value
            } yield address -> value
          }
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

    (spentList ++ receiveList)
        .groupBy(_.address)
        .mapValues { _.reduce(mergeBalances) }
        .values
  }

  def mergeBalances(a: Balance, b: Balance): Balance = {
    Balance(a.address, spent = a.spent + b.spent, received = a.received + b.received)
  }
}
