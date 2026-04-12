package com.example.concerto.concerto;

import androidx.annotation.NonNull;

import com.example.concerto.models.ConcertoTrack;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConcertoManager {

    private final DatabaseReference db = FirebaseDatabase.getInstance("https://concerto-b02f9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("concertos");
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private ValueEventListener queueListener;
    private ValueEventListener playingListener;
    private ValueEventListener statusListener;

    public interface JoinCallback {
        void onSuccess(boolean isHost);
        void onError(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface QueueUpdateListener {
        void onQueueUpdated(List<ConcertoTrack> queue);
    }

    public interface TrackUpdateListener {
        void onTrackUpdated(ConcertoTrack track);
    }

    public interface StatusUpdateListener {
        void onStatusUpdated(String status);
    }

    // Helper: extract safe Firebase key from Spotify URI
    private String getTrackId(String uri) {
        if (uri == null || uri.trim().isEmpty()) return null;
        return uri.contains(":") ? uri.substring(uri.lastIndexOf(":") + 1) : uri;
    }

    public String getCurrentUserUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void createRoom(String pin, ActionCallback callback) {
        String uid = getCurrentUserUid();
        if (uid == null) {
            if (callback != null) callback.onError("User not logged in.");
            return;
        }

        db.child(pin).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                if (callback != null) callback.onError("PIN already in use. Try another.");
            } else {
                Map<String, Object> roomData = new HashMap<>();
                roomData.put("hostUid", uid);
                roomData.put("status", "active");

                db.child(pin).setValue(roomData).addOnCompleteListener(createTask -> {
                    if (createTask.isSuccessful()) {
                        if (callback != null) callback.onSuccess();
                    } else {
                        if (callback != null) callback.onError("Failed to create room.");
                    }
                });
            }
        });
    }

    public void joinRoom(String pin, JoinCallback callback) {
        String currentUid = getCurrentUserUid();

        db.child(pin).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String status = task.getResult().child("status").getValue(String.class);
                String hostUid = task.getResult().child("hostUid").getValue(String.class);

                if ("active".equals(status)) {
                    boolean isUserTheHost = (currentUid != null && currentUid.equals(hostUid));
                    if (callback != null) callback.onSuccess(isUserTheHost);
                } else if ("host_away".equals(status)) {
                    if (callback != null) callback.onError("The host has stepped out. Try again later.");
                } else {
                    if (callback != null) callback.onError("This session has ended.");
                }
            } else {
                if (callback != null) callback.onError("Invalid PIN. Room not found.");
            }
        });
    }

    public void endRoom(String pin) {
        if (pin != null) {
            db.child(pin).child("status").setValue("ended");
        }
    }

    public void setRoomStatus(String pin, String status) {
        if (pin != null && status != null) {
            db.child(pin).child("status").setValue(status);
        }
    }

    public void toggleVoteForTrack(String pin, ConcertoTrack track) {
        String uid = getCurrentUserUid();
        String trackId = getTrackId(track.uri);

        if (pin != null && uid != null && track != null && trackId != null) {
            DatabaseReference trackRef = db.child(pin).child("queue").child(trackId).child("voters").child(uid);

            if (track.voters != null && track.voters.containsKey(uid)) {
                trackRef.removeValue();
            } else {
                trackRef.setValue(true);
            }
        }
    }

    public void playNextTrack(String pin, List<ConcertoTrack> currentQueue) {
        if (pin != null && currentQueue != null && !currentQueue.isEmpty()) {
            // Skip any corrupt/null-uri tracks at the front of the queue
            ConcertoTrack nextTrack = null;
            String trackId = null;
            for (ConcertoTrack candidate : currentQueue) {
                String id = getTrackId(candidate.uri);
                if (id != null) {
                    nextTrack = candidate;
                    trackId = id;
                    break;
                }
            }
            if (nextTrack != null) {
                db.child(pin).child("currentlyPlaying").setValue(nextTrack);
                db.child(pin).child("queue").child(trackId).removeValue();
            } else {
                db.child(pin).child("currentlyPlaying").removeValue();
            }
        } else if (pin != null) {
            db.child(pin).child("currentlyPlaying").removeValue();
        }
    }

    public void startObservingRoom(String pin, QueueUpdateListener qListener, TrackUpdateListener pListener, StatusUpdateListener sListener) {
        if (pin == null) return;

        stopObservingRoom(pin);

        queueListener = db.child(pin).child("queue").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ConcertoTrack> list = new ArrayList<>();
                for (DataSnapshot trackSnap : snapshot.getChildren()) {
                    String key = trackSnap.getKey();
                    ConcertoTrack track = trackSnap.getValue(ConcertoTrack.class);
                    // Discard and auto-delete corrupt entries (null/empty key or URI)
                    if (key == null || key.trim().isEmpty()
                            || track == null
                            || track.uri == null
                            || track.uri.trim().isEmpty()) {
                        if (key != null && !key.trim().isEmpty()) {
                            trackSnap.getRef().removeValue(); // delete by ref, safe
                        }
                        continue;
                    }
                    list.add(track);
                }
                list.sort((t1, t2) -> {
                    int votes1 = t1.voters != null ? t1.voters.size() : 0;
                    int votes2 = t2.voters != null ? t2.voters.size() : 0;
                    return Integer.compare(votes2, votes1);
                });
                if (qListener != null) qListener.onQueueUpdated(list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        playingListener = db.child(pin).child("currentlyPlaying").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ConcertoTrack track = snapshot.getValue(ConcertoTrack.class);
                if (pListener != null) pListener.onTrackUpdated(track);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        statusListener = db.child(pin).child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if (sListener != null) sListener.onStatusUpdated(status);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void stopObservingRoom(String pin) {
        if (pin != null) {
            if (queueListener != null) {
                db.child(pin).child("queue").removeEventListener(queueListener);
                queueListener = null;
            }
            if (playingListener != null) {
                db.child(pin).child("currentlyPlaying").removeEventListener(playingListener);
                playingListener = null;
            }
            if (statusListener != null) {
                db.child(pin).child("status").removeEventListener(statusListener);
                statusListener = null;
            }
        }
    }

    public void addTrackToQueue(String pin, com.spotify.protocol.types.Track spotifyTrack, ActionCallback callback) {
        String uid = getCurrentUserUid();
        String trackId = getTrackId(spotifyTrack != null ? spotifyTrack.uri : null);

        if (pin == null || spotifyTrack == null || uid == null) {
            if (callback != null) callback.onError("Cannot add track. Missing data.");
            return;
        }

        // Guard: trackId must be non-null (protects against corrupt Spotify track objects)
        if (trackId == null) {
            if (callback != null) callback.onError("Cannot add track: missing track ID.");
            return;
        }

        String artistName = (spotifyTrack.artist != null && spotifyTrack.artist.name != null) ? spotifyTrack.artist.name : "Unknown Artist";
        String imageUrl = (spotifyTrack.imageUri != null && spotifyTrack.imageUri.raw != null) ? spotifyTrack.imageUri.raw : "";

        ConcertoTrack newTrack = new ConcertoTrack(spotifyTrack.uri, spotifyTrack.name, artistName, imageUrl);
        newTrack.voters.put(uid, true);

        // Check if currently playing slot is empty first
        db.child(pin).child("currentlyPlaying").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                // No track playing — auto-play this one
                db.child(pin).child("currentlyPlaying").setValue(newTrack)
                        .addOnCompleteListener(playTask -> {
                            if (playTask.isSuccessful()) {
                                if (callback != null) callback.onSuccess();
                            } else {
                                if (callback != null) callback.onError("Failed to auto-play track.");
                            }
                        });
            } else {
                // FEATURE: Check for duplicate before adding to queue
                db.child(pin).child("queue").child(trackId).get().addOnCompleteListener(dupTask -> {
                    if (dupTask.isSuccessful() && dupTask.getResult().exists()) {
                        if (callback != null) callback.onError("This track is already in the queue!");
                        return;
                    }
                    // Not a duplicate — add to queue
                    db.child(pin).child("queue").child(trackId).setValue(newTrack)
                            .addOnCompleteListener(queueTask -> {
                                if (queueTask.isSuccessful()) {
                                    if (callback != null) callback.onSuccess();
                                } else {
                                    if (callback != null) callback.onError("Failed to add track to queue.");
                                }
                            });
                });
            }
        });
    }

    public void deleteTrackFromQueue(String pin, ConcertoTrack track, ActionCallback callback) {
        String trackId = getTrackId(track != null ? track.uri : null);
        if (pin == null || trackId == null) {
            if (callback != null) callback.onError("Cannot delete track. Missing data.");
            return;
        }
        db.child(pin).child("queue").child(trackId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onSuccess();
                    } else {
                        if (callback != null) callback.onError("Failed to delete track.");
                    }
                });
    }
}