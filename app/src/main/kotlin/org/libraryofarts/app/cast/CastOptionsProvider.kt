package org.libraryofarts.app.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import org.libraryofarts.app.R

/** Cast options for Google TV / Chromecast photo playback. */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(context.getString(R.string.cast_app_id))
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
