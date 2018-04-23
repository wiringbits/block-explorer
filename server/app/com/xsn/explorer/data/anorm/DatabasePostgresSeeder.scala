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
import play.api.db.Database

class DatabasePostgresSeeder @Inject() (
    override val database: Database,
    blockPostgresDAO: BlockPostgresDAO,
    transactionPostgresDAO: TransactionPostgresDAO,
    addressPostgresDAO: BalancePostgresDAO)
    extends DatabaseBlockingSeeder
    with AnormPostgresDataHandler {

  override def firstBlock(command: CreateBlockCommand): ApplicationResult[Unit] = database.withTransaction { implicit conn =>
    val result = upsertBlockCascade(command)

    result
        .map(_ => Good(()))
        .getOrElse(throw new RuntimeException("Unable to add the first block"))
  }


  override def newLatestBlock(command: CreateBlockCommand): ApplicationResult[Unit] = withTransaction { implicit conn =>
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

  override def replaceLatestBlock(command: ReplaceBlockCommand): ApplicationResult[Unit] = withTransaction { implicit conn =>
    val createCommand = CreateBlockCommand(command.newBlock, command.newTransactions)

    val result = for {
      _ <- deleteBlockCascade(command.orphanBlock)
      _ <- upsertBlockCascade(createCommand)
    } yield ()

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to replace latest block"))
  }

  override def insertPendingBlock(command: CreateBlockCommand): ApplicationResult[Unit] = withTransaction { implicit conn =>
    val result = for {
      _ <- upsertBlockCascade(command)
    } yield ()

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to an old block"))
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

  private def deleteBlockCascade(block: Block)(implicit conn: Connection): Option[Unit] = {
    for {
      // block
      _ <- blockPostgresDAO.delete(block.hash)

      // transactions
      deletedTransactions = transactionPostgresDAO.deleteBy(block.hash)

      // balances
      _ <- spendMap(deletedTransactions)
          .map { case (address, value) =>
            val balance = Balance(address, spent = -value)
            addressPostgresDAO.upsert(balance)
          }
          .toList
          .everything

      _ <- receiveMap(deletedTransactions)
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
