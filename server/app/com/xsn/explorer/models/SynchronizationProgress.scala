package com.xsn.explorer.models

case class SynchronizationProgress(total: Int, synced: Int) {
  def missing: Int = Math.max(0, total - synced)
}
