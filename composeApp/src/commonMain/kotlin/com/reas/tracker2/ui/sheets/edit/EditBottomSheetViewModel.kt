package com.reas.tracker2.ui.sheets.edit

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.ScrobbleEdit
import com.reas.tracker2.shared.Track
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.launch

class EditBottomSheetViewModel(
    private val repository: Repository,
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
) : TrackerViewModel() {
    fun edit(scrobble: Scrobble, newTrack: Track) {
        viewModelScope.launch {
            // i don't like this, we do 2x more db calls than necessary
            val trackId = repository.getOrInsertTrack(newTrack)
            val metadataWithIds = repository.getTrack(trackId)

            val newScrobble = scrobble.copy(metadata = metadataWithIds)
            nowPlayingNotificationManager.edit(newScrobble)
            repository.updateScrobbles(newScrobble)
            repository.insertEdit(ScrobbleEdit.edit(scrobble, metadataWithIds, ScrobbleEdit.EditReason.Manual))
        }
    }
}
