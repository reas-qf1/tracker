package com.reas.tracker2.ui.dialogs.delete

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.ScrobbleEdit
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.launch

class DeleteDialogViewModel(
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
    private val repository: Repository,
) : TrackerViewModel() {
    fun delete(scrobble: Scrobble) {
        viewModelScope.launch {
            repository.writeTransaction {
                repository.markScrobbleDeleted(scrobble)
                repository.insertEdit(ScrobbleEdit.delete(scrobble, ScrobbleEdit.EditReason.Manual))
            }
            nowPlayingNotificationManager.delete(scrobble)
        }
    }
}
