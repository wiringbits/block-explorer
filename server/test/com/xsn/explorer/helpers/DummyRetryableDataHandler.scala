package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.async.RetryableDataHandler

class DummyRetryableDataHandler extends RetryableDataHandler {

  def retrying[A](
      f: => FutureApplicationResult[A]
  ): FutureApplicationResult[A] = {
    f
  }
}
