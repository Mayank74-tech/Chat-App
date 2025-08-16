package com.java.chatapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class CallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQ_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private String channelName;
    private boolean isVideo;
    private String remoteUserId;
    private RtcEngine rtcEngine;

    // ⚠ Replace with your actual Agora App ID
    private static final String AGORA_APP_ID = "a3deaaf5013b4086a038a0be15acc91b";
    private static final String AGORA_TOKEN = null; // Use temp/real token when App Certificate is enabled

    private FrameLayout localContainer, remoteContainer;
    private ImageButton btnEnd;
    private ImageView btnMute, btnSwitch;
    private boolean isMuted = false;

    private final IRtcEngineEventHandler eventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() ->
                    Toast.makeText(CallActivity.this, "Joined channel: " + channel, Toast.LENGTH_SHORT).show()
            );
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                clearRemoteVideo();
                Toast.makeText(CallActivity.this, "User left the call", Toast.LENGTH_SHORT).show();
                endCall();
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        // Get Intent extras
        channelName = getIntent().getStringExtra("channelName");
        isVideo = getIntent().getBooleanExtra("isVideo", true);
        remoteUserId = getIntent().getStringExtra("remoteUserId");

        // Bind views
        localContainer = findViewById(R.id.local_video_view_container);
        remoteContainer = findViewById(R.id.remote_video_view_container);
        btnEnd = findViewById(R.id.btnEndCall);
        btnMute = findViewById(R.id.btnMute);
        btnSwitch = findViewById(R.id.btnSwitch);

        // Set UI visibility based on call type
        btnSwitch.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        localContainer.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        // Button actions
        btnEnd.setOnClickListener(v -> endCall());
        btnMute.setOnClickListener(v -> toggleMute());
        btnSwitch.setOnClickListener(v -> switchCamera());

        // Request permissions if needed
        if (hasAllPermissions()) {
            initAgoraAndJoin();
        } else {
            ActivityCompat.requestPermissions(this, REQ_PERMISSIONS, PERMISSION_REQ_ID);
        }
    }

    private boolean hasAllPermissions() {
        for (String p : REQ_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initAgoraAndJoin() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AGORA_APP_ID;
            config.mEventHandler = eventHandler;

            rtcEngine = RtcEngine.create(config);
            rtcEngine.setEnableSpeakerphone(true);

            if (isVideo) {
                rtcEngine.enableVideo();
                setupLocalVideo();
                rtcEngine.startPreview();
            } else {
                rtcEngine.disableVideo();
                rtcEngine.enableAudio();
            }

            ChannelMediaOptions options = new ChannelMediaOptions();
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;

            rtcEngine.joinChannel(AGORA_TOKEN, channelName, 0, options);

        } catch (Exception e) {
            Toast.makeText(this, "Agora init error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupLocalVideo() {
        if (!isVideo) return;
        SurfaceView localView = RtcEngine.CreateRendererView(getBaseContext());
        localContainer.removeAllViews();
        localContainer.addView(localView);
        rtcEngine.setupLocalVideo(new VideoCanvas(localView, VideoCanvas.RENDER_MODE_FIT, 0));
    }

    private void setupRemoteVideo(int uid) {
        SurfaceView remoteView = RtcEngine.CreateRendererView(getBaseContext());
        remoteContainer.removeAllViews();
        remoteContainer.addView(remoteView);
        rtcEngine.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_FIT, uid));
    }

    private void clearRemoteVideo() {
        remoteContainer.removeAllViews();
    }

    private void toggleMute() {
        isMuted = !isMuted;
        rtcEngine.muteLocalAudioStream(isMuted);
        btnMute.setAlpha(isMuted ? 0.5f : 1.0f); // visual feedback
    }

    private void switchCamera() {
        if (isVideo) {
            rtcEngine.switchCamera();
        }
    }

    private void leaveAndCleanupAgora() {
        if (rtcEngine != null) {
            try {
                rtcEngine.stopPreview();
            } catch (Exception ignored) {}
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    private void removeCallNodes() {
        FirebaseDatabase.getInstance().getReference("calls")
                .child(FirebaseAuth.getInstance().getUid())
                .removeValue();

        if (remoteUserId != null) {
            FirebaseDatabase.getInstance().getReference("calls")
                    .child(remoteUserId)
                    .removeValue();
        }
    }

    private void endCall() {
        removeCallNodes();
        leaveAndCleanupAgora();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveAndCleanupAgora();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            boolean granted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                initAgoraAndJoin();
            } else {
                Toast.makeText(this, "Camera/Mic permission required for call", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
