package com.reas.tracker2.internalsServer

import com.reas.tracker2.shared.*
import io.github.oshai.kotlinlogging.KotlinLogging

class LocalServer(
    private val repository: Repository,
    private val eventProcessor: EventProcessor,
    private val holePlugger: HolePlugger,
) : ServerContract {
    private val logger = KotlinLogging.logger { }

    suspend fun startup() {
        holePlugger.cancelAll()
        holePlugger.register(repository.getNowPlaying())
    }

    override suspend fun submit(events: List<RawEvent>): List<Scrobble> =
        repository.writeTransaction {
            val registeredEvents = events.map { event -> register(event) }
            val (newScrobbles, updatedScrobbles, pluggedScrobbles) = eventProcessor.process(registeredEvents)
            logger.debug {
                "${newScrobbles.size} new scrobbles, " +
                    "${updatedScrobbles.size} updated scrobbles, " +
                    "${pluggedScrobbles.size} plugged scrobbles"
            }

            repository.insertScrobbles(newScrobbles)
            repository.updateScrobbleEvents(updatedScrobbles + pluggedScrobbles)
            repository.insertEdits(newScrobbles.map { play -> ScrobbleEdit.new(play) })

            holePlugger.register(newScrobbles)
            holePlugger.register(updatedScrobbles)
            holePlugger.register(pluggedScrobbles)

            return@writeTransaction (newScrobbles + updatedScrobbles)
                .filter { scrobble -> !scrobble.isDeleted }
                .map { refreshEdits(it) }
        }

    private fun separateArtists(artists: String): List<String> = artists.split(" & ")

    private suspend fun registerArtists(artists: RawArtistList): ArtistList {
        val artistsSep = artists.list ?: separateArtists(artists.raw)
        val existingArtistMap = repository.getArtists(artistsSep).associate { it.name to it.id }
        val newArtists = artistsSep.filter { !existingArtistMap.containsKey(it) }
        if (newArtists.isEmpty()) {
            return ArtistList(
                artistsSep.map { Artist(it, existingArtistMap.getValue(it)) },
                artists.raw,
            )
        }
        val newIds = repository.insertArtists(newArtists)
        val newArtistMap = newArtists.zip(newIds).toMap()
        return ArtistList(
            artistsSep.map { Artist(it, newArtistMap[it] ?: existingArtistMap.getValue(it)) },
            artists.raw,
        )
    }

    private suspend fun registerAlbum(name: String, artists: RawArtistList): Album {
        val artists = registerArtists(artists)
        return repository.getAlbum(name, artists) ?: run {
            val albumId = repository.insertAlbum(name, artists)
            repository.associateArtistsWithAlbum(albumId, artists)
            Album(name, artists, albumId)
        }
    }

    private suspend fun registerTrack(
        name: String,
        artists: RawArtistList,
        albumName: String?,
        albumArtists: RawArtistList?,
    ): Track {
        val artistList = registerArtists(artists)
        val album = albumName?.let { registerAlbum(albumName, albumArtists ?: artists) }
        return repository.getTrack(name, artistList, album) ?: run {
            val trackId = repository.insertTrack(name, artistList, album)
            repository.associateArtistsWithTrack(trackId, artistList)
            Track(album, name, artistList, trackId)
        }
    }

    suspend fun register(event: RawEvent): Event =
        Event(
            metadata = register(event.metadata),
            duration = event.duration,
            info = event.info,
            source = event.source,
        )

    // TODO: make private when edit contract is implemented
    suspend fun register(metadata: RawMetadata): Track =
        repository.writeTransaction {
            registerTrack(metadata.track, metadata.artists, metadata.album, metadata.albumArtists)
        }

    private suspend fun refreshEdits(scrobble: Scrobble): Scrobble =
        repository.getScrobble(scrobble.id)
            ?: throw IllegalStateException("can't find scrobble by id ${scrobble.id}")
}
