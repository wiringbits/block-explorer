package com.xsn.explorer.modules

import com.xsn.explorer.tasks.CurrencySynchronizerTask
import play.api.inject.{SimpleModule, bind}

class CurrencySynchronizerModule
    extends SimpleModule(bind[CurrencySynchronizerTask].toSelf.eagerly())
