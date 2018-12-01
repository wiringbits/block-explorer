package controllers.common

import org.scalactic.Every
import play.api.libs.json._

object Codecs {

  implicit def everyReads[T](implicit readsT: Reads[T]): Reads[Every[T]] = Reads[Every[T]] { json =>
    json
      .validate[List[T]]
      .flatMap { list =>
        Every.from(list)
          .map(JsSuccess.apply(_))
          .getOrElse {
            JsError.apply("A non-empty list is expected")
          }
      }
  }
}