package com.java.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class IncomingCallActivity extends AppCompatActivity {

    private String callerId, callerName, callerImage, channelName, callType;
    private boolean isVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        // Get UI elements
        TextView tvCallerName = findViewById(R.id.tvCallerName);
        ImageView ivCallerImage = findViewById(R.id.ivCallerImage);
        Button btnAccept = findViewById(R.id.btnAccept);
        Button btnReject = findViewById(R.id.btnReject);

        // Get data from Intent
        callerId = getIntent().getStringExtra("callerId");
        callerName = getIntent().getStringExtra("callerName");
        callerImage = getIntent().getStringExtra("callerImage");
        channelName = getIntent().getStringExtra("channelName");
        callType = getIntent().getStringExtra("callType"); // "audio" or "video"
        isVideo = "video".equalsIgnoreCase(callType);

        // Set caller details
        tvCallerName.setText(callerName != null ? callerName : "Unknown Caller");
        if (callerImage != null && !callerImage.isEmpty()) {
            Glide.with(this).load(callerImage).into(ivCallerImage);
        }

        // Accept Call
        btnAccept.setOnClickListener(v -> {
            // Update Firebase: call accepted
            FirebaseDatabase.getInstance().getReference("calls")
                    .child(callerId)
                    .child("status")
                    .setValue("accepted");

            Intent intent = new Intent(this, CallActivity.class);
            intent.putExtra("channelName", channelName);
            intent.putExtra("isVideo", isVideo);
            intent.putExtra("remoteUserId", callerId);
            startActivity(intent);
            finish();
        });

        // Reject Call
        btnReject.setOnClickListener(v -> {
            // Update Firebase: call rejected
            FirebaseDatabase.getInstance().getReference("calls")
                    .child(callerId)
                    .child("status")
                    .setValue("rejected");

            // Remove call entry for receiver
            FirebaseDatabase.getInstance().getReference("calls")
                    .child(FirebaseAuth.getInstance().getUid())
                    .removeValue();

            finish();
        });
    }
}
