package com.reas.tracker2.ui.dialogs.delete

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.Play
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.launch

class DeleteDialogViewModel(
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
    private val repository: Repository,
) : TrackerViewModel() {
    fun delete(play: Play) {
        viewModelScope.launch {
            repository.markPlayDeleted(play)
            nowPlayingNotificationManager.delete(play)
        }
    }
}
