package com.reas.tracker2.ui.dialogs.delete

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.Play
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.launch

class DeleteDialogViewModel(
    private val eventProcessor: EventProcessor,
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
    private val repository: Repository,
) : TrackerViewModel() {
    fun delete(play: Play) {
        viewModelScope.launch {
            if (eventProcessor.addTemporaryEdit(play, null)) {
                nowPlayingNotificationManager.showDefault()
            }
            repository.deletePlay(play)
        }
    }
}
