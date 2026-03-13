package oss.krtirtho.spotube.automotive

import android.content.ComponentName
import android.content.Intent
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import oss.krtirtho.spotube.MainActivity

/**
 * Car App Library screen that shows Spotube's current playback state and
 * provides play/pause/skip controls via the car host's template UI.
 *
 * Connects to the existing audio_service MediaBrowserService so all controls
 * go through the same media session that powers the full Flutter UI.
 * Tapping "Open App" launches the full Spotube Flutter UI directly.
 */
class NowPlayingScreen(carContext: CarContext) : Screen(carContext) {

    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private var metadata: MediaMetadataCompat? = null
    private var playbackState: PlaybackStateCompat? = null
    private var isConnecting = true

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    connectMediaBrowser()
                }

                override fun onStop(owner: LifecycleOwner) {
                    disconnectMediaBrowser()
                }
            }
        )
    }

    // ── MediaBrowser connection ──────────────────────────────────────────────

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            val browser = mediaBrowser ?: return
            try {
                val controller = MediaControllerCompat(carContext, browser.sessionToken)
                controller.registerCallback(mediaControllerCallback)
                metadata = controller.metadata
                playbackState = controller.playbackState
                mediaController = controller
            } catch (e: android.os.RemoteException) {
                // AudioService session token not yet active; show idle state
            }
            isConnecting = false
            invalidate()
        }

        override fun onConnectionFailed() {
            isConnecting = false
            invalidate()
        }

        override fun onConnectionSuspended() {
            mediaController?.unregisterCallback(mediaControllerCallback)
            mediaController = null
            metadata = null
            playbackState = null
            invalidate()
        }
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(newMetadata: MediaMetadataCompat?) {
            metadata = newMetadata
            invalidate()
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState = state
            invalidate()
        }
    }

    // ── Car App Library template ─────────────────────────────────────────────

    override fun onGetTemplate(): Template {
        val ctrl = mediaController
        val meta = metadata
        val state = playbackState

        // "Open App" launches the full Spotube Flutter Activity
        val openAppAction = Action.Builder()
            .setTitle("Open App")
            .setOnClickListener {
                carContext.startActivity(
                    Intent(carContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            .build()

        if (isConnecting) {
            return PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                .setTitle("Spotube")
                .setHeaderAction(Action.APP_ICON)
                .setActionStrip(ActionStrip.Builder().addAction(openAppAction).build())
                .build()
        }

        val title = meta?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            ?: "Nothing Playing"
        val artist = meta?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = state?.state == PlaybackStateCompat.STATE_PLAYING

        // When there is no active media session, only the "Open App" action is shown
        // so the user can launch the full Flutter UI to start playback.
        if (ctrl == null) {
            val rowBuilder = Row.Builder().setTitle(title)
            if (artist.isNotBlank()) rowBuilder.addText(artist)
            return PaneTemplate.Builder(Pane.Builder().addRow(rowBuilder.build()).build())
                .setTitle("Spotube")
                .setHeaderAction(Action.APP_ICON)
                .setActionStrip(ActionStrip.Builder().addAction(openAppAction).build())
                .build()
        }

        val prevAction = Action.Builder()
            .setTitle("Prev")
            .setOnClickListener { ctrl.transportControls.skipToPrevious() }
            .build()

        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "Pause" else "Play")
            .setOnClickListener {
                if (isPlaying) ctrl.transportControls.pause()
                else ctrl.transportControls.play()
            }
            .build()

        val nextAction = Action.Builder()
            .setTitle("Next")
            .setOnClickListener { ctrl.transportControls.skipToNext() }
            .build()

        val rowBuilder = Row.Builder().setTitle(title)
        if (artist.isNotBlank()) rowBuilder.addText(artist)
        val row = rowBuilder.build()

        return PaneTemplate.Builder(Pane.Builder().addRow(row).build())
            .setTitle("Spotube")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(prevAction)
                    .addAction(playPauseAction)
                    .addAction(nextAction)
                    .addAction(openAppAction)
                    .build()
            )
            .build()
    }

    private fun connectMediaBrowser() {
        mediaBrowser?.disconnect()
        isConnecting = true
        mediaBrowser = MediaBrowserCompat(
            carContext,
            ComponentName(
                carContext.packageName,
                "com.ryanheise.audioservice.AudioService"
            ),
            connectionCallback,
            null
        ).also { it.connect() }
        invalidate()
    }

    private fun disconnectMediaBrowser() {
        mediaController?.unregisterCallback(mediaControllerCallback)
        mediaController = null
        mediaBrowser?.disconnect()
        mediaBrowser = null
        metadata = null
        playbackState = null
        isConnecting = false
    }
}
