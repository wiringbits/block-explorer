package com.xsn.explorer.modules

import com.xsn.explorer.tasks.SQSSeederTask
import play.api.inject.{SimpleModule, _}

class SeederModule extends SimpleModule(bind[SQSSeederTask].toSelf.eagerly())
