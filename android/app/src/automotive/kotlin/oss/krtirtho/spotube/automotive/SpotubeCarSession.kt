package oss.krtirtho.spotube.automotive

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Car App Library session — creates the initial screen shown by the AAOS car host.
 */
class SpotubeCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = NowPlayingScreen(carContext)
}
