package com.xsn.explorer.util

import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult, FutureOr}
import com.alexitc.playsonify.models.ApplicationError
import org.scalactic.{Bad, Good, One}

import scala.concurrent.{ExecutionContext, Future}

object Extensions {

  private val SatoshiScale = 100000000L

  implicit class BigDecimalExt(val inner: BigDecimal) extends AnyVal {

    def fromSatoshis: BigDecimal = {
      inner / SatoshiScale
    }

    def toSatoshis: BigInt = {
      (inner * SatoshiScale).toBigInt
    }
  }

  implicit class ListOptionExt[+A](val inner: List[Option[A]]) extends AnyVal {

    def everything: Option[List[A]] = {
      if (inner.forall(_.isDefined)) {
        Some(inner.flatten)
      } else {
        None
      }
    }
  }

  implicit class FutureApplicationResultListExt[A](
      val inner: List[FutureApplicationResult[A]]
  ) extends AnyVal {

    def sequence(implicit
        ec: ExecutionContext
    ): FutureApplicationResult[List[A]] = {
      Future.sequence(inner).map { list =>
        val partial = list
          .foldLeft[ApplicationResult[List[A]]](Good(List.empty[A])) {
            case (Bad(accErrors), Bad(curErrors)) => Bad(accErrors ++ curErrors)
            case (Bad(accErrors), _) => Bad(accErrors)
            case (_, Bad(curErrors)) => Bad(curErrors)
            case (Good(accList), Good(cur)) => Good(cur :: accList)
          }

        partial.map(_.reverse)
      }
    }
  }

  implicit class FutureOrExt[+A](val inner: FutureOr[A]) {

    def recoverFrom[B >: A](
        error: ApplicationError
    )(f: => B)(implicit ec: ExecutionContext): FutureOr[B] = {
      val future = inner.toFuture.map {
        case Good(result) => Good(result)
        case Bad(One(e)) if e == error => Good(f)
        case Bad(errors) => Bad(errors)
      }

      new FutureOr(future)
    }

    def recoverWith[B >: A](
        error: ApplicationError
    )(f: => FutureOr[B])(implicit ec: ExecutionContext): FutureOr[B] = {
      val future = inner.toFuture.flatMap {
        case Good(result) => Future.successful(Good(result))
        case Bad(One(e)) if e == error => f.toFuture
        case Bad(errors) => Future.successful(Bad(errors))
      }

      new FutureOr(future)
    }
  }
}
