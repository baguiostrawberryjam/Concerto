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

    public void toggleVoteForTrack(String pin, ConcertoTrack track) {
        String uid = getCurrentUserUid();
        if (pin != null && uid != null && track != null) {
            DatabaseReference trackRef = db.child(pin).child("queue").child(track.uri).child("voters").child(uid);

            if (track.voters != null && track.voters.containsKey(uid)) {
                trackRef.removeValue();
            } else {
                trackRef.setValue(true);
            }
        }
    }

    public void playNextTrack(String pin, List<ConcertoTrack> currentQueue) {
        if (pin != null && currentQueue != null && !currentQueue.isEmpty()) {
            ConcertoTrack nextTrack = currentQueue.get(0);
            db.child(pin).child("currentlyPlaying").setValue(nextTrack);
            db.child(pin).child("queue").child(nextTrack.uri).removeValue();
        } else if (pin != null) {
            db.child(pin).child("currentlyPlaying").removeValue();
        }
    }

    public void startObservingRoom(String pin, QueueUpdateListener qListener, TrackUpdateListener pListener, StatusUpdateListener sListener) {
        if (pin == null) return;

        // FIXED: Always clear old listeners before attaching new ones to prevent ghost data leaks
        stopObservingRoom(pin);

        queueListener = db.child(pin).child("queue").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ConcertoTrack> list = new ArrayList<>();
                for (DataSnapshot trackSnap : snapshot.getChildren()) {
                    ConcertoTrack track = trackSnap.getValue(ConcertoTrack.class);
                    if (track != null) list.add(track);
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

        if (pin == null || spotifyTrack == null || uid == null) {
            if (callback != null) callback.onError("Cannot add track. Missing data.");
            return;
        }

        String artistName = (spotifyTrack.artist != null && spotifyTrack.artist.name != null) ? spotifyTrack.artist.name : "Unknown Artist";
        String imageUrl = (spotifyTrack.imageUri != null && spotifyTrack.imageUri.raw != null) ? spotifyTrack.imageUri.raw : "";

        ConcertoTrack newTrack = new ConcertoTrack(spotifyTrack.uri, spotifyTrack.name, artistName, imageUrl);
        newTrack.voters.put(uid, true);

        db.child(pin).child("currentlyPlaying").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                db.child(pin).child("currentlyPlaying").setValue(newTrack)
                        .addOnCompleteListener(playTask -> {
                            if (playTask.isSuccessful()) {
                                if (callback != null) callback.onSuccess();
                            } else {
                                if (callback != null) callback.onError("Failed to auto-play track.");
                            }
                        });
            } else {
                db.child(pin).child("queue").child(spotifyTrack.uri).setValue(newTrack)
                        .addOnCompleteListener(queueTask -> {
                            if (queueTask.isSuccessful()) {
                                if (callback != null) callback.onSuccess();
                            } else {
                                if (callback != null) callback.onError("Failed to add track to queue.");
                            }
                        });
            }
        });
    }
}