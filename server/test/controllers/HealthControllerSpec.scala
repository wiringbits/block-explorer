package controllers

import controllers.common.MyAPISpec
import play.api.Application
import play.api.test.Helpers._

class HealthControllerSpec extends MyAPISpec {

  val application: Application = guiceApplicationBuilder.build()

  "GET /health" should {
    "return OK" in {
      val response = GET("/health")

      status(response) mustEqual OK
    }
  }
}
