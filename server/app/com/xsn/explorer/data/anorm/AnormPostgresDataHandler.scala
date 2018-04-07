package com.xsn.explorer.data.anorm

import java.sql.Connection

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.errors.{PostgresError, PostgresForeignKeyViolationError, UnknownPostgresError}
import org.postgresql.util.PSQLException
import org.scalactic.Bad
import play.api.db.Database

/**
 * Allow us to map a [[PSQLException]] to a sub type of [[PostgresError]].
 *
 * This is helpful to differentiate between errors caused by input data
 * and failures that can not be prevented, these failures are thrown.
 *
 * The errors are mapped based on postgres error codes:
 * - see: https://www.postgresql.org/docs/9.6/static/errcodes-appendix.html
 */
trait AnormPostgresDataHandler {

  protected def database: Database

  def withConnection[A](block: Connection => ApplicationResult[A]): ApplicationResult[A] = {
    try {
      database.withConnection(block)
    } catch {
      case e: PSQLException if isIntegrityConstraintViolationError(e) =>
        val error = createForeignKeyViolationError(e).getOrElse(UnknownPostgresError(e))
        Bad(error).accumulating
    }
  }

  def withTransaction[A](block: Connection => ApplicationResult[A]): ApplicationResult[A] = {
    try {
      database.withTransaction(block)
    } catch {
      case e: PSQLException if isIntegrityConstraintViolationError(e) =>
        val error = createForeignKeyViolationError(e).getOrElse(UnknownPostgresError(e))
        Bad(error).accumulating
    }
  }

  private def isIntegrityConstraintViolationError(e: PSQLException) = e.getSQLState startsWith "23"
  private def createForeignKeyViolationError(e: PSQLException): Option[PostgresError] = {
    // assumes not null
    val detail = e.getServerErrorMessage.getDetail

    // expected format = [Key (column)=(given_value) is not present in table "table".]
    val regex = raw"Key (.*)=.*".r
    detail match {
      case regex(dirtyColumn, _*) =>
        val column = dirtyColumn.substring(1, dirtyColumn.length - 1)
        val error = PostgresForeignKeyViolationError(column, e)
        Some(error)

      case _ => None
    }
  }
}
