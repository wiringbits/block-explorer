package com.xsn.explorer.services

import com.sendgrid._
import javax.inject.Inject
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

trait EmailService {
  def sendEmail(subject: String, text: String, recipients: List[String]): Unit
}

object EmailService {

  object Disabled extends EmailService {
    private val logger = LoggerFactory.getLogger(this.getClass)
    override def sendEmail(
        subject: String,
        text: String,
        recipients: List[String]
    ): Unit = {
      logger.info(
        s"Not sending email, subject = $subject, recipients = ${recipients
          .mkString(", ")}, text = $text"
      )
    }
  }

  class SendgridService @Inject() (config: SendgridService.Config)
      extends EmailService {

    private val logger = LoggerFactory.getLogger(this.getClass)
    private val sendgrid = new SendGrid(config.apiKey)

    override def sendEmail(
        subject: String,
        text: String,
        recipients: List[String]
    ): Unit = {
      Try(
        unsafeSendEmail(subject = subject, text = text, recipients = recipients)
      ) match {
        case Failure(exception) =>
          logger.warn(s"Failed to send email: $text", exception)
        case Success(_) =>
          // log this so that we get notified about it on sentry, shouldn't happen frequently anyway
          logger.warn(s"Email sent on monitored addresses: $text")
      }
    }

    private def unsafeSendEmail(
        subject: String,
        text: String,
        recipients: List[String]
    ): Unit = {
      if (recipients.isEmpty) {
        throw new RuntimeException("There are no recipients for the email")
      }
      val from = new Email(config.sender)
      val to = new Email("test@example.com")
      val content = new Content("text/plain", text)
      val mail = new Mail(from, subject, to, content)

      // set recipients
      val personalization = new Personalization()
      mail.personalization = null
      recipients.foreach { recipient =>
        personalization.addTo(new Email(recipient))
      }
      mail.addPersonalization(personalization)

      val request = new Request
      request.setMethod(Method.POST)
      request.setEndpoint("mail/send")
      request.setBody(mail.build)

      val _ = sendgrid.api(request)
    }
  }

  object SendgridService {
    case class Config(apiKey: String, sender: String)
  }
}
