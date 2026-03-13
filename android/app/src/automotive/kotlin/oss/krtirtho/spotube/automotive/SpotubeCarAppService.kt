package oss.krtirtho.spotube.automotive

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Car App Library entry point for Android Automotive OS.
 *
 * Provides the AAOS system with a car-safe Now Playing interface backed by the
 * existing audio_service MediaBrowserService. The full Spotube Flutter UI is still
 * launched when the user opens the app from the AAOS app launcher.
 */
class SpotubeCarAppService : CarAppService() {
    // ALLOW_ALL_HOSTS_VALIDATOR is intentional for an internal-testing app:
    // OEM automotive hosts (GM, etc.) do not publish their signing certificates
    // publicly, making a restricted HostValidator impractical here.
    // If you promote to public release, replace with HostValidator.Builder()
    // configured with the certified OEM host certificates for your target vehicles.
    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = SpotubeCarSession()
}
