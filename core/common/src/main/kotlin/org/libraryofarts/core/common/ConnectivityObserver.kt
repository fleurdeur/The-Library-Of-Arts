package org.libraryofarts.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class ConnectivityObserver(private val context: Context) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        if (manager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            private fun update() {
                trySend(isCurrentlyOnline(manager))
            }

            override fun onAvailable(network: Network) = update()
            override fun onLost(network: Network) = update()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = update()
        }
        manager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback,
        )
        trySend(isCurrentlyOnline(manager))
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    fun isCurrentlyOnline(): Boolean =
        isCurrentlyOnline(context.getSystemService(ConnectivityManager::class.java))

    private fun isCurrentlyOnline(manager: ConnectivityManager?): Boolean {
        if (manager == null) return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
