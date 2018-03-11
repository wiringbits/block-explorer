package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.services.{XSNService, XSNServiceRPCImpl}

class XSNServiceModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[XSNService]).to(classOf[XSNServiceRPCImpl])
  }
}
