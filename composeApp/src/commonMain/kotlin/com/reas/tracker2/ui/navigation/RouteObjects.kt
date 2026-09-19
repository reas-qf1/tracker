package com.reas.tracker2.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.reas.tracker2.shared.Album
import com.reas.tracker2.shared.Artist
import com.reas.tracker2.shared.TrackWithAlbum
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import tracker2.composeapp.generated.resources.*

@Serializable
sealed class Route : NavKey

@Serializable
sealed class DialogRoute : Route()

@Serializable
enum class ChartType {
    ARTISTS {
        override val icon: ImageVector
            get() = Icons.Filled.Person
        override val label: StringResource
            get() = Res.string.artists
    },
    ALBUMS {
        override val icon: ImageVector
            get() = Icons.Filled.Album
        override val label: StringResource
            get() = Res.string.albums
    },
    TRACKS {
        override val icon: ImageVector
            get() = Icons.Filled.MusicNote
        override val label: StringResource
            get() = Res.string.tracks
    },
    ;

    @Transient abstract val icon: ImageVector

    @Transient abstract val label: StringResource
}

@Serializable
enum class ChartSort {
    TIME {
        override val icon: ImageVector
            get() = Icons.Filled.AccessTime
        override val label: StringResource
            get() = Res.string.time_played
        override val byTime = true
    },
    PLAYS {
        override val icon: ImageVector
            get() = Icons.Filled.PlayArrow
        override val label: StringResource
            get() = Res.string.plays
        override val byTime = false
    },
    ;

    @Transient abstract val icon: ImageVector

    @Transient abstract val label: StringResource

    @Transient abstract val byTime: Boolean
}

@Serializable
object History : Route()

@Serializable
data class TrackHistory(
    val track: TrackWithAlbum,
) : Route()

@Serializable
data class Charts(
    val type: ChartType = ChartType.ARTISTS,
) : Route()

@Serializable
object Settings : Route()

@Serializable
object Debug : Route()

@Serializable
data class ArtistInfo(
    val artist: Artist,
) : Route()

@Serializable
data class AlbumInfo(
    val album: Album,
) : Route()

@Serializable
data class TrackInfo(
    val track: TrackWithAlbum,
) : Route()

@Serializable
data class BottomSheetInfo(
    val artist: Artist? = null,
    val album: Album? = null,
    val track: TrackWithAlbum? = null,
) : DialogRoute()

@Serializable
data class Error(
    val message: String,
) : DialogRoute()
