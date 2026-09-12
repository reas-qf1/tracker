package com.reas.tracker2.ui.viewmodels

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.Play
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.ui.components.printShort
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

    suspend fun getImageUrl(scrobble: Play): String? {
        scrobble.asAlbum?.let { album ->
            return networkRepository.getAlbumImageUrl(album, "large")
        }
        return null
    }

    suspend fun delete(scrobble: Play) {
        eventProcessor.addTemporaryEdit(scrobble, null)
        repository.deletePlay(scrobble)
    }

    suspend fun edit(scrobble: Play, newMetadata: TrackWithAlbum) {
        eventProcessor.addTemporaryEdit(scrobble, newMetadata)
        repository.updatePlay(scrobble.copy(metadata = newMetadata))
    }
}