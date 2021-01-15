package controllers.common

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.play.AbstractAuthenticatorService
import org.scalactic.Good
import play.api.libs.json.JsValue
import play.api.mvc.Request

import scala.concurrent.Future

class MyAuthenticatorService extends AbstractAuthenticatorService[Nothing] {

  @com.github.ghik.silencer.silent
  override def authenticate(
      request: Request[JsValue]
  ): FutureApplicationResult[Nothing] = {
    Future.successful(Good(throw new RuntimeException("Not implemented")))
  }
}
