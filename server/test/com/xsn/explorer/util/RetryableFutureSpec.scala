package com.xsn.explorer.util

import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.Future
import akka.actor.{ActorSystem, Scheduler}

import scala.concurrent.duration._
import scala.util.{Failure, Try}

@com.github.ghik.silencer.silent
class RetryableFutureSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    actorSystem.terminate()
    ()
  }

  class UnstableOperation(numberOfFailures: Int, error: Throwable) {
    var tryCount: Int = 0
    var lastExecutionTime: Long = 0
    private var delays: List[FiniteDuration] = List.empty[FiniteDuration]

    def getDelays = {
      delays.reverse
    }

    def execute: Future[Int] = {
      if (lastExecutionTime > 0) {
        delays = (System.currentTimeMillis - lastExecutionTime).millis :: delays
      }
      lastExecutionTime = System.currentTimeMillis

      Future {
        if (tryCount == numberOfFailures) {
          1
        } else {
          tryCount = tryCount + 1
          throw error
        }
      }
    }
  }

  object UnstableOperation {

    def apply(numberOfFailures: Int, error: Throwable): UnstableOperation = {
      new UnstableOperation(numberOfFailures, error)
    }
  }

  val actorSystem = ActorSystem()
  implicit val scheduler: Scheduler = actorSystem.scheduler

  private val retryOnRuntimeException = (result: Try[Int]) => {
    result match {
      case Failure(_: RuntimeException) => true
      case _ => false
    }
  }

  private val threeRetries = List.fill(3)(100.millis)
  private val withThreeRetriesOnRuntimeException =
    RetryableFuture(threeRetries)(retryOnRuntimeException)(_)

  "RetryableFuture" should {
    "succeed after retrying" in {
      val operation = UnstableOperation(3, new RuntimeException)

      val result = withThreeRetriesOnRuntimeException {
        operation.execute
      }

      result.map(_ must be(1))
    }

    "fail after too many retries" in {
      val operation = UnstableOperation(4, new RuntimeException)

      val result = withThreeRetriesOnRuntimeException {
        operation.execute
      }

      result.failed.map(_ => 1 must be(1))
    }

    "retry only when shouldRetry returns true" in {
      val operation = UnstableOperation(2, new Exception)

      val result = withThreeRetriesOnRuntimeException {
        operation.execute
      }

      result.failed.map(_ => 1 must be(1))
    }

    "succeed when operation does not need to be retried" in {
      val operation = UnstableOperation(0, new Exception)

      val result = withThreeRetriesOnRuntimeException {
        operation.execute
      }

      result.map(_ must be(1))
    }
  }

  "withExponentialBackoff" should {
    "Calculate the right delays with initial delay and max delay" in {
      val operation = UnstableOperation(4, new RuntimeException)
      val retryWithExponentialBackoff =
        RetryableFuture.withExponentialBackoff[Int](50.millis, 500.millis)
      val result = retryWithExponentialBackoff(retryOnRuntimeException) {
        operation.execute
      }

      val expecetedDelays = List(50.millis, 100.millis, 200.millis, 400.millis)

      result.map(_ =>
        expecetedDelays
          .zip(operation.getDelays)
          .map { case (d1, d2) =>
            isCloseTo(d1.toMillis, d2.toMillis)
          }
          .forall(a => a) must be(true)
      )
    }

    "Calculate the right delays with initial delay and max retries" in {
      val operation = UnstableOperation(4, new RuntimeException)
      val retryWithExponentialBackoff =
        RetryableFuture.withExponentialBackoff[Int](50.millis, 4)
      val result = retryWithExponentialBackoff(retryOnRuntimeException) {
        operation.execute
      }

      val expecetedDelays = List(50.millis, 100.millis, 200.millis, 400.millis)

      result.map(_ =>
        expecetedDelays
          .zip(operation.getDelays)
          .map { case (d1, d2) =>
            isCloseTo(d1.toMillis, d2.toMillis)
          }
          .forall(a => a) must be(true)
      )
    }
  }

  "getDelay" should {
    "return base delay with retry zero" in {
      val delay = RetryableFuture.getDelay(100, 0)
      isCloseTo(delay.toMillis, 100) must be(true)
    }

    "should double the base delay with each retry by default" in {
      val delay = RetryableFuture.getDelay(100, 2)
      isCloseTo(delay.toMillis, 400) must be(true)
    }

    "should grow the base delay according to the supplied factor" in {
      val delay = RetryableFuture.getDelay(100, 2, factor = 4)
      isCloseTo(delay.toMillis, 1600) must be(true)
    }
  }

  private def isCloseTo(n1: Long, n2: Long, delta: Long = 130) = {
    Math.abs(n1 - n2) <= delta
  }
}
