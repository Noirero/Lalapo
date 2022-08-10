// AM -->
package eu.kanade.tachiyomi.data.connections

import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy

abstract class ConnectionsService(val id: Long) {

    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()

    open val client: OkHttpClient
        get() = networkService.client

    // Name of the connection service to display
    @StringRes
    abstract fun nameRes(): Int

    @DrawableRes
    abstract fun getLogo(): Int

    @ColorInt
    abstract fun getLogoColor(): Int

    @CallSuper
    open fun logout() {
        preferences.connectionToken(this).set("")
    }

    open val isLogged: Boolean
        get() = preferences.connectionToken(this).get().isNotEmpty()
}
// AM <--
