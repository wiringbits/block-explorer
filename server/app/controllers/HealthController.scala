package controllers

import javax.inject.Inject

import controllers.common.{MyJsonController, MyJsonControllerComponents}

class HealthController @Inject() (
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def check() = Action {
    Ok
  }
}
