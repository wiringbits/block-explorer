package com.xsn.explorer.modules

import com.google.inject.{AbstractModule, Provides}
import com.xsn.explorer.config.NotificationsConfig
import com.xsn.explorer.models.values.Address
import com.xsn.explorer.services.EmailService
import javax.inject.Singleton
import org.slf4j.LoggerFactory
import play.api.Configuration

class EmailModule extends AbstractModule {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def configure(): Unit = {}

  @Provides
  @Singleton
  def notificationsConfig(globalConfig: Configuration): NotificationsConfig = {
    val config = globalConfig.get[Configuration]("notifications")
    val monitoredAddresses =
      getList(config, "monitoredAddresses").flatMap(Address.from)
    val recipients = getList(config, "recipients")

    logger.info(
      s"notifications config loaded, monitoredAddresses = ${monitoredAddresses
        .mkString(", ")}, recipients = ${recipients
        .mkString(", ")}"
    )
    NotificationsConfig(
      recipients = recipients,
      monitoredAddresses = monitoredAddresses
    )
  }

  @Provides
  @Singleton
  def sendgridConfig(
      globalConfig: Configuration
  ): EmailService.SendgridService.Config = {
    val config = globalConfig.get[Configuration]("sendgrid")
    val apiKey = config.get[String]("apiKey")
    val sender = config.get[String]("sender")

    logger.info(
      s"sendgrid config loaded, apiKey = ${apiKey.take(4)}..., sender = $sender"
    )
    EmailService.SendgridService.Config(apiKey = apiKey, sender = sender)
  }

  @Provides
  @Singleton
  def emailService(
      config: EmailService.SendgridService.Config
  ): EmailService = {
    new EmailService.SendgridService(config)
  }

  private def getList(config: Configuration, key: String): List[String] = {
    config.get[String](key).split(",").map(_.trim).toList
  }
}
