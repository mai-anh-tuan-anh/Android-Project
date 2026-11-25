package com.example.myapplication.data.remote;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.lifecycle.LiveData;

/** Tracks whether Android has validated Internet access on the current default network. */
public final class CatalogConnectivity extends LiveData<Boolean> {
    private final ConnectivityManager manager;
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override public void onLost(Network network) { postValue(false); }
        @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
            postValue(validated(capabilities));
        }
    };

    public CatalogConnectivity(ConnectivityManager manager) {
        this.manager = manager;
    }

    @Override protected void onActive() {
        Network current = manager.getActiveNetwork();
        setValue(current != null && validated(manager.getNetworkCapabilities(current)));
        manager.registerDefaultNetworkCallback(callback);
    }

    @Override protected void onInactive() {
        manager.unregisterNetworkCallback(callback);
    }

    private static boolean validated(NetworkCapabilities capabilities) {
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
