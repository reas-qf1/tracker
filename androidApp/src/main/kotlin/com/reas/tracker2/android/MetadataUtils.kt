package com.reas.tracker2.android

import android.media.MediaMetadata

val MediaMetadata.title
    get() = this.getString(MediaMetadata.METADATA_KEY_TITLE)
val MediaMetadata.artist
    get() = this.getString(MediaMetadata.METADATA_KEY_ARTIST)
val MediaMetadata.album
    get() = this.getString(MediaMetadata.METADATA_KEY_ALBUM)
val MediaMetadata.albumArtist
    get() = this.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
val MediaMetadata.duration
    get() = this.getLong(MediaMetadata.METADATA_KEY_DURATION)