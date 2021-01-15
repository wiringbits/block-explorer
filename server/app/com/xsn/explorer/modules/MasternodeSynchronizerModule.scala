package com.xsn.explorer.modules

import com.xsn.explorer.tasks.MasternodeSynchronizerTask
import play.api.inject.{SimpleModule, bind}

class MasternodeSynchronizerModule
    extends SimpleModule(bind[MasternodeSynchronizerTask].toSelf.eagerly())
