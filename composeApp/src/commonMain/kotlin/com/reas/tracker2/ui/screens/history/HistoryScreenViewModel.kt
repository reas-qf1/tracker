package com.reas.tracker2.ui.screens.history

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.printShort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class HistoryEntry {
    abstract fun key(): String

    data class Play(
        val scrobble: Scrobble,
    ) : HistoryEntry() {
        override fun key(): String = scrobble.key
    }

    data class Separator(
        val text: String,
    ) : HistoryEntry() {
        override fun key(): String = text
    }
}

class HistoryScreenViewModel(
    private val repository: Repository,
    private val networkRepository: NetworkRepository,
) : TrackerViewModel() {
    val history: Flow<PagingData<HistoryEntry>>
        get() =
            pagingDataFlow { repository.getRecentScrobbles() }
                .mapElements { entity ->
                    HistoryEntry.Play(entity.toObject())
                }.map {
                    it.insertSeparators { before, after ->
                        if (before == null) return@insertSeparators null
                        if (after == null) return@insertSeparators null
                        val beforeDate = before.scrobble.timestamp.printShort()
                        val afterDate = after.scrobble.timestamp.printShort()
                        if (beforeDate != afterDate) {
                            HistoryEntry.Separator(afterDate)
                        } else {
                            null
                        }
                    }
                }

    suspend fun getImageUrl(scrobble: Scrobble): String? {
        scrobble.asAlbum?.let { album ->
            return networkRepository.getAlbumImageUrl(album, "large")
        }
        return null
    }
}
