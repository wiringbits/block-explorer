package com.xsn.explorer.util

import com.alexitc.playsonify.core.FutureOr
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
      (inner * SatoshiScale).toBigInt()
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

  implicit class FutureOrExt[+A](val inner: FutureOr[A]) {
    def recoverFrom[B >: A](error: ApplicationError)(f: => B)(implicit ec: ExecutionContext): FutureOr[B] = {
      val future = inner.toFuture.map {
        case Good(result) => Good(result)
        case Bad(One(e)) if e == error => Good(f)
        case Bad(errors) => Bad(errors)
      }

      new FutureOr(future)
    }

    def recoverWith[B >: A](error: ApplicationError)(f: => FutureOr[B])(implicit ec: ExecutionContext): FutureOr[B] = {
      val future = inner.toFuture.flatMap {
        case Good(result) => Future.successful(Good(result))
        case Bad(One(e)) if e == error => f.toFuture
        case Bad(errors) => Future.successful(Bad(errors))
      }

      new FutureOr(future)
    }
  }
}
