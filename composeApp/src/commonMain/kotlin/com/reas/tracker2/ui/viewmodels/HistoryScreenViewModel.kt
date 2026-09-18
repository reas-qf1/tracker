package com.reas.tracker2.ui.viewmodels

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.Play
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.ui.components.printShort
import com.reas.tracker2.util.NowPlayingNotificationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class HistoryEntry {
    abstract fun key(): String
    data class Play(val play: com.reas.tracker2.shared.Play): HistoryEntry() {
        override fun key(): String = play.key
    }
    data class Separator(val text: String): HistoryEntry() {
        override fun key(): String = text
    }
}

class HistoryScreenViewModel(
    private val repository: Repository,
    private val networkRepository: NetworkRepository,
    private val eventProcessor: EventProcessor,
    private val nowPlayingNotificationManager: NowPlayingNotificationManager,
): TrackerViewModel() {
    val history: Flow<PagingData<HistoryEntry>>
        get() = pagingDataFlow { repository.getRecentPlays() }
            .mapElements { entity ->
                HistoryEntry.Play(repository.playEntityToObject(entity))
            }
            .map { it.insertSeparators { before, after ->
                if (before == null) return@insertSeparators null
                if (after == null) return@insertSeparators null
                val beforeDate = before.play.timestamp.printShort()
                val afterDate = after.play.timestamp.printShort()
                if (beforeDate != afterDate) {
                    HistoryEntry.Separator(afterDate)
                } else {
                    null
                }
            } }

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
        val newScrobble = play.copy(metadata = newMetadata)
        if (eventProcessor.addTemporaryEdit(play, newMetadata)) {
            nowPlayingNotificationManager.show(newScrobble)
        }
        repository.updatePlay(newScrobble)
    }
}