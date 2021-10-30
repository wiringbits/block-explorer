package com.xsn.explorer.modules

import com.xsn.explorer.tasks.PollerSynchronizerTask
import play.api.inject.{SimpleModule, bind}

class PollerSynchronizerModule extends SimpleModule(bind[PollerSynchronizerTask].toSelf.eagerly())
