package controllers.common

import com.alexitc.playsonify.play.{I18nPlayService, JsonControllerComponents, PublicErrorRenderer}
import javax.inject.Inject
import play.api.mvc.MessagesControllerComponents

import scala.concurrent.ExecutionContext

class MyJsonControllerComponents @Inject() (
    override val messagesControllerComponents: MessagesControllerComponents,
    override val executionContext: ExecutionContext,
    override val publicErrorRenderer: PublicErrorRenderer,
    override val i18nService: I18nPlayService,
    override val authenticatorService: MyAuthenticatorService)
    extends JsonControllerComponents[Nothing]
