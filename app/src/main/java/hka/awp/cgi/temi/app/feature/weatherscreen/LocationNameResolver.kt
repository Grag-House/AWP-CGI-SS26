package hka.awp.cgi.temi.app.feature.weatherscreen

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

interface LocationNameResolver {
    suspend fun resolveLocationName(latitude: Double, longitude: Double): String?
}

class GeocoderLocationNameResolver(context: Context) : LocationNameResolver {
    private val appContext = context.applicationContext

    @Suppress("DEPRECATION")
    override suspend fun resolveLocationName(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                return@withContext null
            }

            runCatching {
                Geocoder(appContext, Locale.getDefault())
                    /* this synchronous call is no problem, since this is run inside of IO threads
                     (Since this will run on android api level 23,
                     there is no async version yet)
                     */
                    .getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull()
            }.getOrNull()?.let { address ->
                address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.countryName
            }
        }
    }
}
