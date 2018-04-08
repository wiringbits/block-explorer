package com.xsn.explorer.config

import javax.inject.Inject

import play.api.Configuration

trait SeederConfig {

  def queueUrl: String

  def isEnabled: Boolean
}

class PlaySeederConfig @Inject() (configuration: Configuration) extends SeederConfig {

  override def queueUrl: String = {
    configuration.get[String]("seeder.queueUrl")
  }

  override def isEnabled: Boolean = {
    configuration.get[Boolean]("seeder.enabled")
  }
}
