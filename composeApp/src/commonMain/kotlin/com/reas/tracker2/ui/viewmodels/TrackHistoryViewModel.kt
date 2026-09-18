package com.reas.tracker2.ui.viewmodels

import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.Play
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.flow.map

class TrackHistoryViewModel(
    private val repository: Repository,
    private val networkRepository: NetworkRepository,
    private val eventProcessor: EventProcessor,
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
): TrackerViewModel() {
    fun history(track: TrackWithAlbum) =
        pagingDataFlow { repository.getTrackHistory(track) }
            .mapElements { entity -> repository.playEntityToObject(entity) }

    fun trackPlays(track: TrackWithAlbum) =
        repository.getTrackPlays(track, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    suspend fun getImageUrl(play: Play): String? {
        play.asAlbum?.let { album ->
            return networkRepository.getAlbumImageUrl(album, "large")
        }
        return null
    }

    suspend fun delete(play: Play) {
        if (eventProcessor.addTemporaryEdit(play, null)) {
            nowPlayingNotificationManager.showDefault()
        }
        repository.deletePlay(play)
    }

    suspend fun edit(play: Play, newMetadata: TrackWithAlbum) {
        val newPlay = play.copy(metadata = newMetadata)
        if (eventProcessor.addTemporaryEdit(play, newMetadata)) {
            nowPlayingNotificationManager.show(newPlay)
        }
        repository.updatePlay(newPlay)
    }
}