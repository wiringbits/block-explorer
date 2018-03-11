package controllers.common

import com.alexitc.playsonify.AbstractJsonController
import com.alexitc.playsonify.models.{ErrorId, ServerError}
import org.slf4j.LoggerFactory

abstract class MyJsonController(cc: MyJsonControllerComponents) extends AbstractJsonController[Nothing](cc) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  override def onServerError(error: ServerError, errorId: ErrorId): Unit = {
    val message = s"Unexpected server error, id = ${errorId.string}, error = $error"

    error
        .cause
        .orElse {
          logger.warn(message)
          None
        }
        .foreach { cause =>
          // we'll log as error when there is an exception involved
          logger.error(message, cause)
        }
  }
}
