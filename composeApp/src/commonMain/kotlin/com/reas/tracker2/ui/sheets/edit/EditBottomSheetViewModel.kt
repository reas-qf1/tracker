package com.reas.tracker2.ui.sheets.edit

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.database.AppRepository
import com.reas.tracker2.internalsServer.LocalServer
import com.reas.tracker2.shared.RawMetadata
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.ScrobbleEdit
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.launch

class EditBottomSheetViewModel(
    private val repository: AppRepository,
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
    private val server: LocalServer,
) : TrackerViewModel() {
    fun edit(scrobble: Scrobble, newMetadata: RawMetadata) {
        viewModelScope.launch {
            val newTrack = server.register(newMetadata)
            val newScrobble = scrobble.copy(metadata = newTrack)
            nowPlayingNotificationManager.edit(newScrobble)
            repository.writeTransaction {
                repository.updateScrobbles(newScrobble)
                repository.insertEdit(ScrobbleEdit.edit(scrobble, newTrack, ScrobbleEdit.EditReason.Manual))
            }
        }
    }
}
