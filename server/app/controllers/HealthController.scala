package controllers

import com.xsn.explorer.services.StatisticsService
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import org.scalactic.Good

class HealthController @Inject()(cc: MyJsonControllerComponents, statsService: StatisticsService)
    extends MyJsonController(cc) {

  def check() = Action.async { _ =>
    statsService.getSynchronizationProgress
      .map {
        case Good(progress) if progress.missing < 10 => Ok
        case Good(progress) => InternalServerError(s"There are ${progress.missing} missing blocks")
        case _ => InternalServerError("Failed to check sync progress")
      }
      .recover {
        case _: Throwable => InternalServerError("Failed to check sync progress")
      }
  }
}
