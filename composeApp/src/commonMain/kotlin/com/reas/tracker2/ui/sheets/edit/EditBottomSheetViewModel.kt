package com.reas.tracker2.ui.sheets.edit

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.Play
import com.reas.tracker2.shared.PlayEdit
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.launch

class EditBottomSheetViewModel(
    private val repository: Repository,
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
) : TrackerViewModel() {
    fun edit(play: Play, newMetadata: TrackWithAlbum) {
        viewModelScope.launch {
            // i don't like this, we do 2x more db calls than necessary
            val trackId = repository.getOrInsertTrack(newMetadata)
            val metadataWithIds = repository.getTrack(trackId)

            val newScrobble = play.copy(metadata = metadataWithIds)
            nowPlayingNotificationManager.edit(newScrobble)
            repository.updatePlay(newScrobble)
            repository.insertEdit(PlayEdit.edit(play, metadataWithIds, PlayEdit.EditReason.Manual))
        }
    }
}
