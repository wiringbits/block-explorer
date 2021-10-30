package com.xsn.explorer.modules

import com.xsn.explorer.tasks.DatabaseMigrationsTask
import play.api.inject.{SimpleModule, bind}

@com.github.ghik.silencer.silent
class DatabaseMigrationsModule extends SimpleModule(bind[DatabaseMigrationsTask].toSelf.eagerly())
