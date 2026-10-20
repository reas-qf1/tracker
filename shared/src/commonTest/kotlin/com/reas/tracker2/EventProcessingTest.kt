package com.reas.tracker2

import com.reas.tracker2.shared.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class EventProcessingTest {
    private val testSource = Source.local("test")
    private val testClock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(0)
    }
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private suspend fun TestScope.setupAndTest(actions: suspend EventProcessor.() -> Unit): List<Scrobble> {
        val adapter = TestEventProcessorAdapter()
        val eventProcessor = EventProcessor(adapter)
        val holePlugger = HolePlugger(this, testClock)
        val processJob = launch {
            eventProcessor.processQueue()
        }
        val plays = mutableMapOf<String, Scrobble>()
        val collectJob = launch {
            eventProcessor.collectPlays { newPlays ->
                newPlays.forEach { play ->
                    plays[play.key] = play
                }
                holePlugger.register(newPlays)
            }
        }
        val plugJob = launch {
            holePlugger.collectPlays { play ->
                plays[play.key] = play
            }
        }
        eventProcessor.actions()
        processJob.cancel()
        collectJob.cancel()
        plugJob.cancel()
        return plays.values.toList()
    }

    private suspend fun EventProcessor.add(vararg events: Event) {
        addEvents(events.toList())
        delay(1.milliseconds) // to let all jobs run
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun EventProcessor.addFromFile(name: String) {
        val file = File("src/commonTest/kotlin/com/reas/tracker2/$name")
        val events = json.decodeFromStream<List<EventRepr>>(file.inputStream())
        addEvents(
            events.map { event ->
                Event.create(
                    track = event.track,
                    artist = event.artist,
                    album = event.album,
                    albumArtist = event.album_artist,
                    timestamp = event.timestamp,
                    duration = event.duration,
                    isPlaying = event.is_playing == 1,
                    position = event.position,
                    source = Source(
                        user = event.source_user,
                        client = event.source_device,
                        app = event.source_app,
                    ),
                )
            },
        )
        delay(1.milliseconds) // to let all jobs run
    }

    private fun Event.Companion.createTest(
        isPlaying: Boolean,
        timestamp: Duration,
        position: Duration,
        track: String = "track",
        artist: String = "artist",
        album: String? = "album",
        albumArtist: String? = "albumArtist",
        duration: Duration = 10.seconds,
        source: Source = testSource,
    ) = Event.create(
        track = track,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        duration = duration.inWholeMilliseconds,
        timestamp = (testClock.now() + timestamp).toEpochMilliseconds(),
        position = position.inWholeMilliseconds,
        isPlaying = isPlaying,
        source = source,
    )

    @Test
    fun basicTest() =
        runTest {
            val plays = setupAndTest {
                add(
                    Event.createTest(true, 0.seconds, 0.seconds),
                    Event.createTest(false, 1.seconds, 1.seconds),
                )
            }
            assertEquals(1, plays.size)
            assertEquals(1.seconds, plays[0].timePlayed)
            assertEquals(testClock.now(), plays[0].timestamp)
        }

    @Test
    fun basicPlugHoleTest() =
        runTest {
            val plays = setupAndTest {
                add(Event.createTest(true, 0.seconds, 0.seconds))
                delay(100.seconds)
            }
            assertEquals(1, plays.size)
            assertEquals(10.seconds, plays[0].timePlayed)
        }

    @Test
    fun basicFromFileTest() =
        runTest {
            val plays = setupAndTest {
                addFromFile("test.json")
            }
            assertEquals(1, plays.size)
            assertEquals("memories", plays[0].track)
        }
}
