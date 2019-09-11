package com.xsn.explorer.data.async

import java.sql.{SQLException, SQLTransientConnectionException}

import akka.actor.Scheduler
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.google.inject.Inject
import com.xsn.explorer.config.RetryConfig
import com.xsn.explorer.util.RetryableFuture
import org.postgresql.util.PSQLException

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Try}

trait RetryableDataHandler {
  def retrying[A](f: => FutureApplicationResult[A]): FutureApplicationResult[A]
}

class RetryableFutureDataHandler @Inject()(retryConfig: RetryConfig)(
    implicit ec: ExecutionContext,
    scheduler: Scheduler
) extends RetryableDataHandler {

  override def retrying[A](f: => FutureApplicationResult[A]): FutureApplicationResult[A] = {
    val retry = RetryableFuture.withExponentialBackoff[ApplicationResult[A]](
      retryConfig.initialDelay,
      retryConfig.maxDelay
    )
    val shouldRetry: Try[ApplicationResult[A]] => Boolean = {
      case Failure(_: SQLTransientConnectionException) =>
        true
      case Failure(e: PSQLException) if e.getMessage contains "the database system is starting up" =>
        true
      case Failure(e: PSQLException) if e.getMessage contains "An I/O error occurred while sending to the backend" =>
        true
      case Failure(e: SQLException) if e.getMessage contains "Connection is closed" =>
        true
      case _ =>
        false
    }

    retry(shouldRetry) {
      f
    }
  }
}
