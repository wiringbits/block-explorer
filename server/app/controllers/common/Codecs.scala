package controllers.common

import com.alexitc.playsonify.models.pagination.PaginatedResult
import com.xsn.explorer.models._
import org.scalactic.Every
import play.api.libs.json._

object Codecs {

  implicit def everyReads[T](implicit readsT: Reads[T]): Reads[Every[T]] = Reads[Every[T]] { json =>
    json
      .validate[List[T]]
      .flatMap { list =>
        Every
          .from(list)
          .map(JsSuccess.apply(_))
          .getOrElse {
            JsError.apply("A non-empty list is expected")
          }
      }
  }

  implicit def paginatedResultWrites[T](implicit writesT: Writes[T]): Writes[PaginatedResult[T]] =
    OWrites[PaginatedResult[T]] { result =>
      Json.obj(
        "offset" -> result.offset,
        "limit" -> result.limit,
        "total" -> result.total,
        "data" -> result.data
      )
    }
}
