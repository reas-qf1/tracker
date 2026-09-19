package com.reas.tracker2.android

import android.app.Notification
import android.content.Context
import android.text.Html
import androidx.work.*
import com.reas.tracker2.R
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.TimePeriod
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class DailyReportWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val repository: Repository by inject()
    private val notif: NotificationWrapper by inject()

    override suspend fun doWork(): Result {
        val period =
            TimePeriod(
                clock.now() - 1.days,
                clock.now(),
            )
        val artists = repository.getMostPlayedArtists(period, limit = COUNT).first()
        val albums = repository.getMostPlayedAlbums(period, limit = COUNT).first()
        val tracks = repository.getMostPlayedTracks(period, limit = COUNT).first()

        if (artists.isNotEmpty()) {
            notif.show("Daily Report") {
                setSmallIcon(R.drawable.ic_stat_name)
                setContentTitle(applicationContext.getString(R.string.daily_report_header))
                style =
                    Notification.BigTextStyle().bigText(
                        Html.fromHtml(
                            """
                                        <b>${
                                applicationContext.getString(R.string.daily_report_artists)
                            }</b><br>${
                                artists.joinToString("<br>") { it.artist.name }
                            }<br>
                                        <b>${
                                applicationContext.getString(R.string.daily_report_albums)
                            }</b><br>${
                                albums.joinToString("<br>") { "${it.album.artistsAsString} - ${it.album.name}" }
                            }<br>
                                        <b>${
                                applicationContext.getString(R.string.daily_report_tracks)
                            }</b><br>${
                                tracks.joinToString("<br>") { "${it.track.artistsAsString} - ${it.track.name}" }
                            }
                            """.trimIndent(),
                        ),
                    )
            }
        }
        return Result.success()
    }

    companion object {
        private const val COUNT = 3
        private val clock = Clock.System
        private val logger = KotlinLogging.logger {}

        fun start(context: Context) {
            val calendar: Calendar = Calendar.getInstance()
            val nowMillis: Long = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 2)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1)
            }
            val diff = calendar.timeInMillis - nowMillis
            logger.debug { "Work will run in $diff milliseconds" }

            val request =
                PeriodicWorkRequestBuilder<DailyReportWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(diff, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    "DailyReportWorker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request,
                )
        }
    }
}
