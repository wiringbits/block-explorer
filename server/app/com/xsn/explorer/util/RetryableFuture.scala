package com.xsn.explorer.util

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.Scheduler
import akka.pattern.Patterns.after

import scala.concurrent.duration._
import scala.util.{Random, Try}

object RetryableFuture {

  /** Retries a future if the result fulfills the specified conditions
    *
    * @param delays
    *   how much time will be waited between each retry, also @delays.length is the number of times the future will be
    *   retried.
    * @param shouldRetry
    *   indicates whether or not retry the future depending on the current result
    * @param f
    *   the future to be retried
    */
  @com.github.ghik.silencer.silent
  def apply[A](
      delays: List[FiniteDuration]
  )(
      shouldRetry: Try[A] => Boolean
  )(
      f: => Future[A]
  )(implicit ec: ExecutionContext, scheduler: Scheduler): Future[A] = {
    delays.foldLeft(f) { case (result, delay) =>
      result.transformWith { t =>
        if (shouldRetry(t)) {
          after(delay, scheduler, ec, Future.successful(1)).flatMap(_ => f)
        } else {
          result
        }
      }
    }
  }

  /** creates a RetryableFuture with delays that double with each retry until max delay is reached
    * @param initialDelay
    *   the delay for the first retry
    * @param maxDelay
    *   the max delay that will be waited while retrying the future
    */
  def withExponentialBackoff[A](
      initialDelay: FiniteDuration,
      maxDelay: FiniteDuration
  )(implicit ec: ExecutionContext, scheduler: Scheduler) = {
    def generateDelays: (Long, Long, Int) => List[Long] =
      (initialDelay: Long, maxDelay: Long, retry: Int) => {
        val currentDelay = getDelay(initialDelay, retry).toMillis
        if (currentDelay > maxDelay) {
          List.empty[Long]
        } else {
          currentDelay :: generateDelays(initialDelay, maxDelay, retry + 1)
        }
      }

    val delays =
      generateDelays(initialDelay.toMillis, maxDelay.toMillis, 0).map(_.millis)
    RetryableFuture[A](delays) _
  }

  /** creates a RetryableFuture with delays that double with each retry
    * @param initialDelay
    *   the delay for the first retry
    * @param maxRetries
    *   the number of times the future will be retried
    */
  def withExponentialBackoff[A](
      initialDelay: FiniteDuration,
      maxRetries: Int
  )(implicit ec: ExecutionContext, scheduler: Scheduler) = {
    val initialDelayMillis = initialDelay.toMillis
    val delays = List.tabulate(maxRetries)(n => getDelay(initialDelayMillis, n))

    RetryableFuture[A](delays) _
  }

  /** calculates the delay for the given retry number
    * @param baseDelay
    *   the delay for the first retry
    * @param retry
    *   the retry for which the delay is being calculated
    * @param factor
    *   how much the delay will grow with each retry. for example: 2 -> double the delay with each retry 3 -> triples
    *   the delay with each retry
    */
  private[util] def getDelay(
      baseDelay: Long,
      retry: Int,
      factor: Int = 2,
      jitter: Int = Random.nextInt(100)
  ): FiniteDuration = {
    ((Math.pow(
      factor.toDouble,
      retry.toDouble
    ) * baseDelay).longValue + jitter).millis
  }
}
