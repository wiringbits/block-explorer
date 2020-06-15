package controllers

import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import org.scalactic.Good
import play.api.libs.json.JsObject

import scala.concurrent.Future

class MaintenanceController @Inject()(components: MyJsonControllerComponents) extends MyJsonController(components) {

  @com.github.ghik.silencer.silent
  def run(query: String) = public { _ =>
    Future.successful(Good(JsObject.empty))
  }
}
