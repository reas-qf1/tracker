package com.reas.tracker2.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.instanceHostName
import com.reas.tracker2.settings.instancePort
import com.reas.tracker2.settings.username
import kotlinx.coroutines.launch

class LoginDialogViewModel(
    private val settings: Settings
) : TrackerViewModel() {
    val hostName
        get() = settings.getBlocking(instanceHostName)
    val port
        get() = settings.getBlocking(instancePort)
    val userName
        get() = settings.getBlocking(username)

    fun setValues(hostName: String, port: Int, userName: String) {
        viewModelScope.launch {
            settings.edit {
                instanceHostName to hostName
                instancePort to port
                username to userName
            }
        }
    }
}