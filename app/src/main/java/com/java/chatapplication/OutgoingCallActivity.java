package com.java.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OutgoingCallActivity extends AppCompatActivity {

    private String calleeId;
    private String channelName;
    private String callType;
    private boolean isVideo;

    private DatabaseReference callsRef;
    private final Handler timeoutHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        // UI
        TextView tvName = findViewById(R.id.tvCalleeName);
        ImageView ivProfile = findViewById(R.id.ivCalleeImage);
        ImageButton btnCancel = findViewById(R.id.btnCancelCall);

        // Firebase reference
        callsRef = FirebaseDatabase.getInstance().getReference("calls");

        // Get data from intent
        calleeId = getIntent().getStringExtra("calleeId");
        String calleeName = getIntent().getStringExtra("calleeName");
        String calleeImage = getIntent().getStringExtra("calleeImage");
        callType = getIntent().getStringExtra("callType"); // "audio" or "video"
        isVideo = "video".equalsIgnoreCase(callType);

        // Generate unique channel name
        channelName = "call_" + System.currentTimeMillis();

        // Show UI
        tvName.setText(calleeName != null ? calleeName : "Unknown User");
        if (calleeImage != null && !calleeImage.isEmpty()) {
            Glide.with(this).load(calleeImage).into(ivProfile);
        }

        // Start call in Firebase
        startCall();

        // Cancel button
        btnCancel.setOnClickListener(v -> endCall("cancelled"));

        // Timeout auto-cancel
        // 30 seconds
        int CALL_TIMEOUT = 30000;
        timeoutHandler.postDelayed(() -> endCall("no_answer"), CALL_TIMEOUT);
    }

    private void startCall() {
        String callerId = FirebaseAuth.getInstance().getUid();

        // Call data
        Map<String, Object> callData = new HashMap<>();
        callData.put("callerId", callerId);
        callData.put("callerName", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getDisplayName());
        callData.put("callerImage", FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString()
                : "");
        callData.put("channelName", channelName);
        callData.put("callType", callType);
        callData.put("status", "ringing");

        // Set for receiver
        callsRef.child(calleeId).setValue(callData);

        // Also keep a record for caller
        assert callerId != null;
        callsRef.child(callerId).setValue(callData);

        // Listen for callee's response
        callsRef.child(calleeId).child("status").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @com.google.firebase.database.annotations.NotNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String status = snapshot.getValue(String.class);

                if ("accepted".equals(status)) {
                    timeoutHandler.removeCallbacksAndMessages(null); // stop timeout
                    Intent intent = new Intent(OutgoingCallActivity.this, CallActivity.class);
                    intent.putExtra("channelName", channelName);
                    intent.putExtra("isVideo", isVideo);
                    intent.putExtra("remoteUserId", calleeId);
                    startActivity(intent);
                    finish();
                } else if ("rejected".equals(status)) {
                    timeoutHandler.removeCallbacksAndMessages(null);
                    endCall("rejected");
                }
            }

            @Override
            public void onCancelled(@NonNull @com.google.firebase.database.annotations.NotNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void endCall(String reason) {
        String callerId = FirebaseAuth.getInstance().getUid();

        // Remove both call records
        assert callerId != null;
        callsRef.child(callerId).removeValue();
        callsRef.child(calleeId).removeValue();

        finish();
    }
}
