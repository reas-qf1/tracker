package com.reas.tracker2.ui.sheets.edit

import androidx.lifecycle.viewModelScope
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.Play
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.launch

class EditBottomSheetViewModel(
    private val repository: Repository,
    private val eventProcessor: EventProcessor,
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
) : TrackerViewModel() {
    fun edit(play: Play, newMetadata: TrackWithAlbum) {
        viewModelScope.launch {
            val newScrobble = play.copy(metadata = newMetadata)
            if (eventProcessor.addTemporaryEdit(play, newMetadata)) {
                nowPlayingNotificationManager.show(newScrobble)
            }
            repository.updatePlay(newScrobble)
        }
    }
}
