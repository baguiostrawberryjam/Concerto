package com.example.concerto.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkMonitor extends ConnectivityManager.NetworkCallback {

    private static NetworkMonitor instance;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);

    private boolean isMonitoring = false;

    private NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkCurrentNetworkState();
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public void startMonitoring() {
        if (isMonitoring) return;
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, this);
        isMonitoring = true;
    }

    public void stopMonitoring() {
        if (!isMonitoring) return;
        connectivityManager.unregisterNetworkCallback(this);
        isMonitoring = false;
    }

    private void checkCurrentNetworkState() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        isConnected.postValue(activeNetwork != null);
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        isConnected.postValue(true);
    }

    @Override
    public void onLost(@NonNull Network network) {
        isConnected.postValue(false);
    }
}