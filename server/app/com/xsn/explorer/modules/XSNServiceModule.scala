package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.services.{XSNService, XSNServiceRPCImpl}

class XSNServiceModule extends AbstractModule {

  override def configure(): Unit = {
    val _ = bind(classOf[XSNService]).to(classOf[XSNServiceRPCImpl])
  }
}
