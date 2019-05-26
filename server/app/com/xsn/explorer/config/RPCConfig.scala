package com.xsn.explorer.config

import javax.inject.Inject

import play.api.Configuration

trait RPCConfig {

  import RPCConfig._

  def host: Host
  def username: Username
  def password: Password
}

object RPCConfig {

  case class Host(string: String) extends AnyVal
  case class Username(string: String) extends AnyVal
  case class Password(string: String) extends AnyVal
}

class PlayRPCConfig @Inject()(config: Configuration) extends RPCConfig {

  import RPCConfig._

  private def get(name: String) = config.get[String](s"rpc.$name")

  override val host: Host = Host(get("host"))

  override def username: Username = Username(get("username"))

  override def password: Password = Password(get("password"))

}
