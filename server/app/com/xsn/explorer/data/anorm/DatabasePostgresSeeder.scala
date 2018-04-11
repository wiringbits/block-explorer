package com.xsn.explorer.data.anorm

import java.sql.Connection
import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.anorm.dao.{BalancePostgresDAO, BlockPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Address, Balance, Transaction}
import com.xsn.explorer.util.Extensions.ListOptionExt
import org.scalactic.Good
import play.api.db.Database

class DatabasePostgresSeeder @Inject() (
    override val database: Database,
    blockPostgresDAO: BlockPostgresDAO,
    transactionPostgresDAO: TransactionPostgresDAO,
    addressPostgresDAO: BalancePostgresDAO)
    extends AnormPostgresDataHandler {

  import DatabasePostgresSeeder._

  def firstBlock(command: CreateBlockCommand): ApplicationResult[Unit] = database.withTransaction { implicit conn =>
    val result = upsertBlockCascade(command)

    result
        .map(_ => Good(()))
        .getOrElse(throw new RuntimeException("Unable to add the first block"))
  }

  /**
   * Creates the new latest block assuming there is a previous block.
   *
   * @param command
   * @return
   */
  def newLatestBlock(command: CreateBlockCommand): ApplicationResult[Unit] = withTransaction { implicit conn =>
    val result = for {
      // link previous block
      previousBlockhash <- command.block.previousBlockhash
      previous <- blockPostgresDAO.getBy(previousBlockhash)
      newPrevious = previous.copy(nextBlockhash = Some(command.block.hash))
      _ <- blockPostgresDAO.upsert(newPrevious)
      _ <- upsertBlockCascade(command)
    } yield ()

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to add the new latest block"))
  }

  def replaceLatestBlock(command: ReplaceBlockCommand): ApplicationResult[Unit] = withTransaction { implicit conn =>
    val deleteCommand = DeleteBlockCommand(command.orphanBlock, command.orphanTransactions)
    val createCommand = CreateBlockCommand(command.newBlock, command.newTransactions)

    val result = for {
      _ <- deleteBlockCascade(deleteCommand)
      _ <- upsertBlockCascade(createCommand)
    } yield ()

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to replace latest block"))
  }

  private def upsertBlockCascade(command: CreateBlockCommand)(implicit conn: Connection): Option[Unit] = {
    for {
      // block
      _ <- blockPostgresDAO.upsert(command.block)

      // transactions
      _ <- command.transactions.map(tx => transactionPostgresDAO.upsert(tx)).everything

      // balances
      _ <- spendMap(command.transactions)
          .map { case (address, value) =>
            val balance = Balance(address, spent = value)
            addressPostgresDAO.upsert(balance)
          }
          .toList
          .everything

      _ <- receiveMap(command.transactions)
          .map { case (address, value) =>
            val balance = Balance(address, received = value)
            addressPostgresDAO.upsert(balance)
          }
          .toList
          .everything
    } yield ()
  }

  private def deleteBlockCascade(command: DeleteBlockCommand)(implicit conn: Connection): Option[Unit] = {
    for {
      // block
      _ <- blockPostgresDAO.delete(command.block.hash)

      // transactions
      _ = command.transactions.foreach(tx => transactionPostgresDAO.delete(tx.id))

      // balances
      _ <- spendMap(command.transactions)
          .map { case (address, value) =>
            val balance = Balance(address, spent = -value)
            addressPostgresDAO.upsert(balance)
          }
          .toList
          .everything

      _ <- receiveMap(command.transactions)
          .map { case (address, value) =>
            val balance = Balance(address, received = -value)
            addressPostgresDAO.upsert(balance)
          }
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
}

object DatabasePostgresSeeder {

  case class CreateBlockCommand(block: Block, transactions: List[Transaction])
  case class DeleteBlockCommand(block: Block, transactions: List[Transaction])
  case class ReplaceBlockCommand(
      orphanBlock: Block, orphanTransactions: List[Transaction],
      newBlock: Block, newTransactions: List[Transaction])
}
