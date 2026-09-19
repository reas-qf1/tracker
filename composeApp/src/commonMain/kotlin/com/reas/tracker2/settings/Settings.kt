package com.reas.tracker2.settings

import com.reas.tracker2.ui.navigation.ChartSort

val isScrobblingEnabled = Setting.boolean(key = "isScrobblingEnabled", default = true)
val chartSort = Setting.enum(key = "chartSort", default = ChartSort.TIME)
