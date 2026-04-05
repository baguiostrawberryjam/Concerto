package com.example.concerto.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.concerto.models.HostedConcerto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    private final DatabaseReference db = FirebaseDatabase.getInstance("https://concerto-b02f9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("concertos");
    private final MutableLiveData<List<HostedConcerto>> hostedConcertosLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<HostedConcerto>> getHostedConcertos() {
        return hostedConcertosLiveData;
    }

    public void fetchUserConcertos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        isLoading.postValue(true); // FIXED: Thread-safe
        String currentUid = user.getUid();

        db.orderByChild("hostUid").equalTo(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<HostedConcerto> list = new ArrayList<>();
                for (DataSnapshot roomSnap : snapshot.getChildren()) {
                    String pin = roomSnap.getKey();
                    String status = roomSnap.child("status").getValue(String.class);
                    if (pin != null && status != null) {
                        list.add(new HostedConcerto(pin, status));
                    }
                }
                hostedConcertosLiveData.postValue(list); // FIXED: Thread-safe
                isLoading.postValue(false);              // FIXED: Thread-safe
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoading.postValue(false);              // FIXED: Thread-safe
            }
        });
    }
}