package com.xsn.explorer.errors

import com.alexitc.playsonify.models.ServerError
import org.postgresql.util.PSQLException

sealed trait PostgresError extends ServerError {

  def psqlException: PSQLException

  override def cause: Option[Throwable] = Option(psqlException)
}

case class UnknownPostgresError(psqlException: PSQLException) extends PostgresError
case class PostgresForeignKeyViolationError(column: String, psqlException: PSQLException) extends PostgresError
