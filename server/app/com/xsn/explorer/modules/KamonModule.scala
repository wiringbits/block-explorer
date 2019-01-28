package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import kamon.Kamon
import kamon.prometheus.PrometheusReporter

class KamonModule extends AbstractModule {

  override def configure(): Unit = {
    val _ = Kamon.addReporter(new PrometheusReporter())
  }
}
