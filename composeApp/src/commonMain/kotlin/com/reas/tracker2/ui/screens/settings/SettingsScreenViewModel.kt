package com.reas.tracker2.ui.screens.settings

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.isScrobblingEnabled
import com.reas.tracker2.ui.TrackerViewModel
import kotlinx.coroutines.launch

class SettingsScreenViewModel(
    private val settings: Settings,
) : TrackerViewModel() {
    val scrobblingEnabledFlow = settings.stateFlow(isScrobblingEnabled)

    fun setScrobblingEnabled(value: Boolean) {
        viewModelScope.launch {
            settings[isScrobblingEnabled] = value
        }
    }
}
