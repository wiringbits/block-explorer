package com.xsn.explorer.modules

import javax.inject.Singleton

import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import com.google.inject.{AbstractModule, Provides}

class AmazonSQSAsyncModule extends AbstractModule {

  override def configure(): Unit = {
  }

  @Provides
  @Singleton
  def createSQSClient: AmazonSQSAsync = {
    AmazonSQSAsyncClientBuilder
        .standard()
        .build()
  }
}
