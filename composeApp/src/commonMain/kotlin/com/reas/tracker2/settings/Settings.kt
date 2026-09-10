package com.reas.tracker2.settings

import com.reas.tracker2.ui.navigation.ChartSort

val isScrobblingEnabled = Setting.boolean(key = "isScrobblingEnabled", default = true)
val instanceHostName = Setting.string(key = "instanceHostName", default = "")
val instancePort = Setting.int(key = "instancePort", default = 0)
val username = Setting.string(key = "username", default = "")
val chartSort = Setting.enum(key = "chartSort", default = ChartSort.TIME)