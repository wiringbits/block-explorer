package com.xsn.explorer.modules

import com.xsn.explorer.tasks.PollingSeederTask
import play.api.inject.{SimpleModule, bind}

class PollingSeederModule extends SimpleModule(bind[PollingSeederTask].toSelf.eagerly())
