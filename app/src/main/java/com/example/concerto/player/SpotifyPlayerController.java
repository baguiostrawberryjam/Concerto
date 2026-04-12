package com.example.concerto.player;

import android.content.Context;

import com.example.concerto.spotify.SpotifyConfig;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class SpotifyPlayerController {

    private SpotifyAppRemote appRemote;
    private final Context context;

    // Guaranteed to use Application context to prevent Activity memory leaks
    public SpotifyPlayerController(Context context) {
        this.context = context.getApplicationContext();
    }

    public interface ConnectionListener {
        void onConnected();
        void onError(Throwable error);
    }

    public interface PlayerStateListener {
        void onTrackChanged(com.spotify.protocol.types.Track track, boolean isPaused);
    }

    public interface ImageListener {
        void onImageLoaded(android.graphics.Bitmap bitmap);
    }

    public void connect(ConnectionListener listener) {
        ConnectionParams params = new ConnectionParams.Builder(SpotifyConfig.CLIENT_ID)
                .setRedirectUri(SpotifyConfig.REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(context, params, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                appRemote = spotifyAppRemote;
                if (listener != null) listener.onConnected();
            }

            @Override
            public void onFailure(Throwable throwable) {
                if (listener != null) listener.onError(throwable);
            }
        });
    }

    public void play(String uri, ConnectionListener listener) {
        if (isReady()) {
            appRemote.getPlayerApi().play(uri)
                    .setResultCallback(empty -> {
                        if (listener != null) listener.onConnected();
                    })
                    .setErrorCallback(throwable -> {
                        if (listener != null) listener.onError(throwable);
                    });
        }
    }

    public void pause() {
        if (isReady()) appRemote.getPlayerApi().pause();
    }

    public void skipNext() {
        if (isReady()) appRemote.getPlayerApi().skipNext();
    }

    public void skipPrevious() {
        if (isReady()) appRemote.getPlayerApi().skipPrevious();
    }

    public void resume() {
        if (isReady()) appRemote.getPlayerApi().resume();
    }

    public void disconnect() {
        if (appRemote != null) {
            SpotifyAppRemote.disconnect(appRemote);
            appRemote = null;
        }
    }

    public void seekTo(long positionMs) {
        if (isReady()) appRemote.getPlayerApi().seekTo(positionMs);
    }

    public void getPlayerState(com.spotify.protocol.client.CallResult.ResultCallback<com.spotify.protocol.types.PlayerState> callback) {
        if (isReady()) appRemote.getPlayerApi().getPlayerState().setResultCallback(callback);
    }

    public void subscribeToPlayerState(PlayerStateListener listener) {
        if (!isReady()) return;

        appRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    if (playerState.track != null && listener != null) {
                        listener.onTrackChanged(playerState.track, playerState.isPaused);
                    }
                });
    }

    public void loadImage(com.spotify.protocol.types.ImageUri imageUri, ImageListener listener) {
        if (!isReady()) return;

        appRemote.getImagesApi()
                .getImage(imageUri)
                .setResultCallback(bitmap -> {
                    if (listener != null) listener.onImageLoaded(bitmap);
                });
    }

    public void playOrConnect(String uri, ConnectionListener listener) {
        if (isReady()) {
            play(uri, listener); // listener.onConnected() is handled inside play()
        } else {
            connect(new ConnectionListener() {
                @Override
                public void onConnected() {
                    play(uri, listener);
                }

                @Override
                public void onError(Throwable error) {
                    if (listener != null) listener.onError(error);
                }
            });
        }
    }

    public boolean isReady() {
        return appRemote != null && appRemote.isConnected();
    }
}