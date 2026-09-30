package com.reas.tracker2.ui.screens.history

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.shared.Play
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.ui.components.printShort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class HistoryEntry {
    abstract fun key(): String

    data class Play(
        val play: com.reas.tracker2.shared.Play,
    ) : HistoryEntry() {
        override fun key(): String = play.key
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
            pagingDataFlow { repository.getRecentPlays() }
                .mapElements { entity ->
                    HistoryEntry.Play(repository.playEntityToObject(entity))
                }.map {
                    it.insertSeparators { before, after ->
                        if (before == null) return@insertSeparators null
                        if (after == null) return@insertSeparators null
                        val beforeDate = before.play.timestamp.printShort()
                        val afterDate = after.play.timestamp.printShort()
                        if (beforeDate != afterDate) {
                            HistoryEntry.Separator(afterDate)
                        } else {
                            null
                        }
                    }
                }

    suspend fun getImageUrl(play: Play): String? {
        play.asAlbum?.let { album ->
            return networkRepository.getAlbumImageUrl(album, "large")
        }
        return null
    }
}
