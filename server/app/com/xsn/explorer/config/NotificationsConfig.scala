package com.xsn.explorer.config

import com.xsn.explorer.models.values.Address

case class NotificationsConfig(
    monitoredAddresses: List[Address],
    recipients: List[String]
)
