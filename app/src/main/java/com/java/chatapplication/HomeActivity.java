package com.java.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.java.chatapplication.adapters.UserAdapter;
import com.java.chatapplication.models.User;

import java.util.*;

public class HomeActivity extends AppCompatActivity {

    private UserAdapter userAdapter;
    private List<User> userList;
    private Map<String, Integer> unreadCountMap;
    private String currentUserId;
    private ImageView ivMenu;

    private ImageButton logoutBtn;
    private DatabaseReference userRef;
    private DatabaseReference messagesRef;
    private DatabaseReference callsRef;
    private DatabaseReference connectedRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        unreadCountMap = new HashMap<>();
        userAdapter = new UserAdapter(this, userList, unreadCountMap, false);
        recyclerView.setAdapter(userAdapter);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users");
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        callsRef = FirebaseDatabase.getInstance().getReference("calls");
        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        loadUsers();
        trackUnreadMessages();
        updateOnlineStatus();
        listenForIncomingCalls();

        ivMenu = findViewById(R.id.ivMenu);
        ivMenu.setOnClickListener(view -> {
            startActivity(new Intent(HomeActivity.this, ProfileSettingsActivity.class));
        });
    }

    private void loadUsers() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();

                Log.d("DEBUG", "Users count: " + snapshot.getChildrenCount());

                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);

                    if (user != null && user.getUid() != null) {
                        if (!user.getUid().equals(currentUserId)) {
                            // Ensure optional fields have defaults
                            if (user.getStatus() == null) user.setStatus("offline");
                            if (user.getUnreadCount() < 0) user.setUnreadCount(0);
                            userList.add(user);
                        }
                    } else {
                        Log.w("DEBUG", "Skipping user: null or missing uid for " + ds.getKey());
                    }
                }

                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DEBUG", "Database error: " + error.getMessage());
            }
        });
    }

    private void trackUnreadMessages() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                unreadCountMap.clear();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                        String receiverId = messageSnapshot.child("receiverId").getValue(String.class);
                        Boolean isSeen = messageSnapshot.child("isSeen").getValue(Boolean.class);
                        String senderId = messageSnapshot.child("senderId").getValue(String.class);

                        if (receiverId != null && receiverId.equals(currentUserId) &&
                                (isSeen == null || !isSeen)) {
                            if (senderId != null) {
                                int count = unreadCountMap.getOrDefault(senderId, 0);
                                unreadCountMap.put(senderId, count + 1);
                            }
                        }
                    }
                }

                userAdapter.setUnreadCountMap(unreadCountMap);
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RealtimeDBError", error.getMessage());
            }
        });
    }

    private void updateOnlineStatus() {
        DatabaseReference userStatusRef = userRef.child(currentUserId).child("status");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    userStatusRef.setValue("online");
                    userStatusRef.onDisconnect().setValue("offline");
                } else {
                    userStatusRef.setValue("offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForIncomingCalls() {
        callsRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String status = snapshot.child("status").getValue(String.class);
                if ("ringing".equals(status)) {
                    Intent intent = new Intent(HomeActivity.this, IncomingCallActivity.class);
                    intent.putExtra("callerId", snapshot.child("callerId").getValue(String.class));
                    intent.putExtra("callerName", snapshot.child("callerName").getValue(String.class));
                    intent.putExtra("callerImage", snapshot.child("callerImage").getValue(String.class));
                    intent.putExtra("channelName", snapshot.child("channelName").getValue(String.class));
                    intent.putExtra("callType", snapshot.child("callType").getValue(String.class));
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
