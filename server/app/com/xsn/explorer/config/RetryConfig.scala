package com.xsn.explorer.config

import scala.concurrent.duration.FiniteDuration

case class RetryConfig(initialDelay: FiniteDuration, maxDelay: FiniteDuration)
