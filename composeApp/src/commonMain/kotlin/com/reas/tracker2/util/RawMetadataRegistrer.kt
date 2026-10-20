package com.reas.tracker2.util

import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.*

class RawMetadataRegistrer(
    private val repository: Repository,
) {
    private fun separateArtists(artists: String): List<String> = artists.split(" & ")

    private suspend fun registerArtists(artistsString: String): ArtistList {
        val artists = separateArtists(artistsString)
        val existingArtistMap = repository.getArtists(artists).associate { it.name to it.id }
        val newArtists = artists.filter { !existingArtistMap.containsKey(it) }
        if (newArtists.isEmpty()) {
            return ArtistList(
                artists.map { Artist(it, existingArtistMap.getValue(it)) },
                artistsString,
            )
        }
        val newIds = repository.insertArtists(newArtists)
        val newArtistMap = newArtists.zip(newIds).toMap()
        return ArtistList(
            artists.map { Artist(it, newArtistMap[it] ?: existingArtistMap.getValue(it)) },
            artistsString,
        )
    }

    private suspend fun registerAlbum(name: String, artistsString: String): Album {
        val artists = registerArtists(artistsString)
        return repository.getAlbum(name, artists) ?: run {
            val albumId = repository.insertAlbum(name, artists)
            repository.associateArtistsWithAlbum(albumId, artists)
            Album(name, artists, albumId)
        }
    }

    suspend fun registerTrack(
        name: String,
        artistsString: String,
        albumName: String?,
        albumArtistsString: String?,
    ): Track {
        val artists = registerArtists(artistsString)
        val album = albumName?.let { registerAlbum(albumName, albumArtistsString ?: artistsString) }
        return repository.getTrack(name, artists, album) ?: run {
            val trackId = repository.insertTrack(name, artists, album)
            repository.associateArtistsWithTrack(trackId, artists)
            Track(album, name, artists, trackId)
        }
    }

    suspend fun register(metadata: RawMetadata): Track =
        repository.writeTransaction {
            registerTrack(metadata.track, metadata.artists, metadata.album, metadata.albumArtists)
        }
}
