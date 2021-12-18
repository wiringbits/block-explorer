package controllers

import controllers.common.{MyJsonController, MyJsonControllerComponents}

import javax.inject.Inject
import org.scalactic.Good
import play.api.libs.json.JsObject

import scala.annotation.nowarn
import scala.concurrent.Future

class MaintenanceController @Inject() (components: MyJsonControllerComponents) extends MyJsonController(components) {

  @nowarn
  def run(query: String) = public { _ =>
    Future.successful(Good(JsObject.empty))
  }
}
