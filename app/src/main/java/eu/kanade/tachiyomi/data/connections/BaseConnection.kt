// AM (CONNECTIONS) -->
package eu.kanade.tachiyomi.data.connections

import androidx.annotation.CallSuper
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy

abstract class BaseConnection(
    override val id: Long,
    override val name: String,
): Connection {

    val connectionsPreferences: ConnectionsPreferences by injectLazy()
    private val networkService: NetworkHelper by injectLazy()

    override val client: OkHttpClient
        get() = networkService.client

    // Name of the connection service to display

    @CallSuper
    override fun logout() {
        connectionsPreferences.setConnectionsCredentials(this, "", "")
        connectionsPreferences.connectionsToken(this).set("")
    }

    override val isLoggedIn: Boolean
        get() = getUsername().isNotEmpty() &&
            getPassword().isNotEmpty()

    override fun getUsername() = connectionsPreferences.connectionsUsername(this).get()

    override fun getPassword() = connectionsPreferences.connectionsPassword(this).get()

    override fun saveCredentials(username: String, password: String) {
        connectionsPreferences.setConnectionsCredentials(this, username, password)
    }
}
// <-- AM (CONNECTIONS)
