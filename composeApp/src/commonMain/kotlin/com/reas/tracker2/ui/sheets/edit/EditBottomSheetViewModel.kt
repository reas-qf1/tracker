package com.reas.tracker2.ui.sheets.edit

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.RawMetadata
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.ScrobbleEdit
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.NowPlayingNotificationManager
import com.reas.tracker2.util.RawMetadataRegistrer
import kotlinx.coroutines.launch

class EditBottomSheetViewModel(
    private val repository: Repository,
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
    private val rawMetadataRegistrer: RawMetadataRegistrer,
) : TrackerViewModel() {
    fun edit(scrobble: Scrobble, newMetadata: RawMetadata) {
        viewModelScope.launch {
            val newTrack = rawMetadataRegistrer.register(newMetadata)
            val newScrobble = scrobble.copy(metadata = newTrack)
            nowPlayingNotificationManager.edit(newScrobble)
            repository.writeTransaction {
                repository.updateScrobbles(newScrobble)
                repository.insertEdit(ScrobbleEdit.edit(scrobble, newTrack, ScrobbleEdit.EditReason.Manual))
            }
        }
    }
}
