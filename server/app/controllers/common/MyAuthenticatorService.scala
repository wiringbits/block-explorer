package controllers.common

import com.alexitc.playsonify.AbstractAuthenticatorService
import com.alexitc.playsonify.core.FutureApplicationResult
import org.scalactic.Good
import play.api.libs.json.JsValue
import play.api.mvc.Request

import scala.concurrent.Future

class MyAuthenticatorService extends AbstractAuthenticatorService[Nothing] {

  override def authenticate(request: Request[JsValue]): FutureApplicationResult[Nothing] = {
    Future.successful(Good(???))
  }
}
