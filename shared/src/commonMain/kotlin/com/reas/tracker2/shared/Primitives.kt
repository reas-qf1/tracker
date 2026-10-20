package com.reas.tracker2.shared

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class ArtistList(
    val artists: List<Artist>,
    val raw: String,
) : List<Artist> {
    val ids = artists.map { it.id }.sorted()

    override fun toString(): String = raw

    override fun equals(other: Any?): Boolean = other is ArtistList && other.artists == artists

    override fun hashCode(): Int = artists.hashCode()

    override val size: Int
        get() = artists.size

    override fun isEmpty(): Boolean = artists.isEmpty()

    override fun contains(element: Artist): Boolean = artists.contains(element)

    override fun iterator(): Iterator<Artist> = artists.iterator()

    override fun containsAll(elements: Collection<Artist>): Boolean = artists.containsAll(elements)

    override fun get(index: Int): Artist = artists[index]

    override fun indexOf(element: Artist): Int = artists.indexOf(element)

    override fun lastIndexOf(element: Artist): Int = artists.lastIndexOf(element)

    override fun listIterator(): ListIterator<Artist> = artists.listIterator()

    override fun listIterator(index: Int): ListIterator<Artist> = artists.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<Artist> = artists.subList(fromIndex, toIndex)
}

@Serializable
data class Artist(
    val name: String,
    val id: Long,
) {
    override fun equals(other: Any?): Boolean = other is Artist && name == other.name

    override fun hashCode(): Int = name.hashCode()
}

@Serializable
data class Album(
    val name: String,
    val artists: ArtistList,
    val id: Long,
) {
    val artistsAsString: String
        get() = artists.raw

    override fun equals(other: Any?): Boolean = other is Album && name == other.name && artists == other.artists

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + artists.hashCode()
        return result
    }
}

@Serializable
data class Track(
    private val albumObject: Album?,
    val name: String,
    val artists: ArtistList,
    val id: Long,
) {
    val albumArtists: ArtistList?
        get() = albumObject?.artists
    val artistsAsString: String
        get() = artists.raw
    val albumArtistsAsString: String?
        get() = albumObject?.artists?.raw
    val album: String?
        get() = albumObject?.name
    val hasAlbum: Boolean
        get() = albumObject != null
    val asAlbum: Album?
        get() = albumObject

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Track) return false
        if (this === other) return true

        if (name != other.name) return false
        if (artists != other.artists) return false
        if (album != other.album) return false
        if (albumArtists != other.albumArtists) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + artists.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + albumArtists.hashCode()
        return result
    }
}

@Serializable
data class RawMetadata(
    val track: String,
    val artists: String,
    val album: String?,
    val albumArtists: String?,
)

@Serializable
enum class EventState {
    PLAYING,
    STOPPED,
    PLUGGED,
}

@Serializable
data class EventInfo(
    val timestamp: Instant,
    val position: Duration,
    val state: EventState,
    val speed: Double,
) {
    val isPlaying: Boolean
        get() = state == EventState.PLAYING
}

@Serializable
data class Event(
    val metadata: Track,
    val duration: Duration,
    val info: EventInfo,
    val source: Source,
) {
    val track: String
        get() = metadata.name
    val artists: ArtistList
        get() = metadata.artists
    val albumArtists: ArtistList?
        get() = metadata.albumArtists
    val artistsAsString: String
        get() = metadata.artists.raw
    val albumArtistsAsString: String?
        get() = metadata.albumArtists?.raw
    val album: String?
        get() = metadata.album
    val user: String
        get() = source.user
    val client: String
        get() = source.client
    val app: String
        get() = source.app
    val timestamp: Instant
        get() = info.timestamp
    val position: Duration
        get() = info.position
    val isPlaying: Boolean
        get() = info.state == EventState.PLAYING
    val state: EventState
        get() = info.state
    val speed: Double
        get() = info.speed

    companion object {
        fun create(
            track: Track,
            duration: Long,
            timestamp: Long,
            position: Long,
            state: EventState,
            speed: Double,
            source: Source,
        ) = Event(
            metadata = track,
            duration = duration.milliseconds,
            info = EventInfo(
                timestamp = Instant.fromEpochMilliseconds(timestamp),
                position = position.milliseconds,
                state = state,
                speed = speed,
            ),
            source = source,
        )
    }
}

@Serializable
data class Source(
    val user: String,
    val client: String,
    val app: String,
) {
    companion object {
        fun user(client: String, app: String) = Source("", client, app)

        fun local(app: String) = Source("", "", app)
    }
}

@Serializable
data class Scrobble(
    val id: Uuid,
    val metadata: Track,
    val isDeleted: Boolean,
    val duration: Duration,
    val timestamp: Instant,
    var timePlayed: Duration,
    val source: Source,
    val associatedEvents: MutableList<EventInfo>,
) {
    val track: String
        get() = metadata.name
    val artists: ArtistList
        get() = metadata.artists
    val albumArtists: ArtistList?
        get() = metadata.albumArtists
    val artistsAsString: String
        get() = metadata.artists.raw
    val albumArtistsAsString: String?
        get() = metadata.albumArtists?.raw
    val album: String?
        get() = metadata.album
    val asAlbum: Album?
        get() = metadata.asAlbum

    val user: String
        get() = source.user
    val client: String
        get() = source.client
    val app: String
        get() = source.app

    val lastEvent: EventInfo
        get() = associatedEvents.last()
    val lastTimestamp
        get() = lastEvent.timestamp
    val lastPosition
        get() = lastEvent.position
    val lastPlaying
        get() = lastEvent.state == EventState.PLAYING
    val isPlugged
        get() = lastEvent.state == EventState.PLUGGED
    val lastSpeed
        get() = lastEvent.speed

    val currentPosition
        get() = lastPosition + (timestamp - lastTimestamp)
    val endTimestamp
        get() = lastEvent.let { lastEvent ->
            lastEvent.timestamp + (duration - lastEvent.position) / lastEvent.speed
        }

    val isNowPlaying
        get() = lastPlaying && currentPosition <= duration
    val isTiny
        get() = timePlayed < EventProcessor.SKIP_MIN_DURATION
    val isSkip
        get() = timePlayed < duration / 2 && timePlayed < 4.minutes

    val key
        get() = "$client/$app/$timestamp"

    fun plug(timestamp: Instant, position: Duration, speed: Double) {
        associatedEvents.add(
            EventInfo(
                timestamp = timestamp,
                position = position,
                speed = speed,
                state = EventState.PLUGGED,
            ),
        )
    }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun fromEvent(event: Event): Scrobble =
            Scrobble(
                id = Uuid.generateV4(),
                metadata = event.metadata,
                isDeleted = false,
                duration = event.duration,
                timestamp = event.timestamp,
                timePlayed = Duration.ZERO,
                source = event.source,
                associatedEvents = mutableListOf(event.info),
            )
    }
}

@Serializable
data class ScrobbleEdit(
    val id: Uuid,
    val scrobbleId: Uuid,
    val timestamp: Instant,
    val track: Track,
    val reason: EditReason,
) {
    @Serializable
    sealed class EditReason {
        @Serializable
        object Initial : EditReason()

        @Serializable
        object Manual : EditReason()
    }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun new(scrobble: Scrobble) =
            ScrobbleEdit(
                id = Uuid.generateV4(),
                scrobbleId = scrobble.id,
                timestamp = scrobble.timestamp,
                track = scrobble.metadata,
                reason = EditReason.Initial,
            )

        @OptIn(ExperimentalUuidApi::class)
        fun edit(scrobble: Scrobble, newTrack: Track, reason: EditReason) =
            ScrobbleEdit(
                id = Uuid.generateV4(),
                scrobbleId = scrobble.id,
                timestamp = Clock.System.now(),
                track = newTrack,
                reason = reason,
            )
    }
}

@Serializable
data class TimePeriod(
    val start: Instant,
    val end: Instant,
) {
    companion object {
        val ALLTIME = TimePeriod(Instant.DISTANT_PAST, Instant.DISTANT_FUTURE)
    }
}
