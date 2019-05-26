package com.xsn.explorer.play

import com.alexitc.playsonify.models.{ErrorId, InternalError, PublicError}
import com.alexitc.playsonify.play.PublicErrorRenderer
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.mvc.Results.{InternalServerError, Status}

import scala.concurrent.Future

class MyHttpErrorHandler @Inject()(errorRenderer: PublicErrorRenderer) extends HttpErrorHandler {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    val publicError = PublicError.genericError(message)
    val error = errorRenderer.renderPublicError(publicError)
    val json = Json.obj(
      "errors" -> Json.arr(error)
    )

    val result = Status(statusCode)(json)
    Future.successful(result)
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    val errorId = ErrorId.create
    val error = errorRenderer.renderPublicError(InternalError(errorId, "Internal error"))
    val json = Json.obj(
      "errors" -> Json.arr(error)
    )

    logger.error(s"Server error, errorId = [${errorId.string}]", exception)
    val result = InternalServerError(json)
    Future.successful(result)
  }
}
