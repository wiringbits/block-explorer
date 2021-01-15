package com.xsn.explorer.modules

import com.xsn.explorer.tasks.MerchantnodeSynchronizerTask
import play.api.inject.{SimpleModule, bind}

class MerchantnodeSynchronizerModule
    extends SimpleModule(bind[MerchantnodeSynchronizerTask].toSelf.eagerly())
