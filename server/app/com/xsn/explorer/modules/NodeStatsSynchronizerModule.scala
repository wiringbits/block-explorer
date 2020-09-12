package com.xsn.explorer.modules

import com.xsn.explorer.tasks.NodeStatsSynchronizerTask
import play.api.inject.{SimpleModule, bind}

class NodeStatsSynchronizerModule extends SimpleModule(bind[NodeStatsSynchronizerTask].toSelf.eagerly())
