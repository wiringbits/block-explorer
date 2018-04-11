package com.xsn.explorer.util

import com.alexitc.playsonify.core.{FutureApplicationResult, FutureOr}
import org.scalactic.{Bad, Good}

import scala.concurrent.{ExecutionContext, Future}

object Extensions {

  private val SatoshiScale = 100000000L

  implicit class BigDecimalExt(val inner: BigDecimal) extends AnyVal {
    def fromSatoshis: BigDecimal = {
      inner / SatoshiScale
    }
  }

  implicit class FutureApplicationResultExt[+A](val inner: List[FutureApplicationResult[A]]) extends AnyVal {
    def toFutureOr(implicit ec: ExecutionContext): FutureOr[List[A]] = {
      val futureList = Future.sequence(inner)

      val future = futureList.map { resultList =>
        val errorsMaybe = resultList
            .flatMap(_.swap.toOption)
            .reduceLeftOption(_ ++ _)
            .map(_.distinct)

        errorsMaybe
            .map(Bad(_))
            .getOrElse {
              val valueList = resultList.flatMap(_.toOption)
              Good(valueList)
            }
      }

      new FutureOr(future)
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
}
