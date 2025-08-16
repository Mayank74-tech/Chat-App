package com.java.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.java.chatapplication.adapters.MessageAdapter;
import com.java.chatapplication.models.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;

    private String currentUserId;
    private String receiverId;

    private MessageAdapter messageAdapter;
    private final List<Message> messageList = new ArrayList<>();

    private DatabaseReference messagesRef;

    private final String appId = "Agora-App-ID ";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        ImageView btnVideoCall = findViewById(R.id.videoCall);
        ImageView btnAudioCall =findViewById(R.id.audioCall);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        assert mAuth.getCurrentUser() != null;
        currentUserId = mAuth.getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("userId");

        recyclerViewMessages = findViewById(R.id.recyclerView);
        editTextMessage = findViewById(R.id.etMessage);
        ImageButton buttonSend = findViewById(R.id.btnSend);

        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this, messageList, currentUserId, null);
        recyclerViewMessages.setAdapter(messageAdapter);

        String chatId = getChatId(currentUserId, receiverId);
        messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);

        TextView tvUsername = findViewById(R.id.Username);
        TextView tvStatus = findViewById(R.id.Status);

        ImageView backbtn = findViewById(R.id.btnBack);
        backbtn.setOnClickListener(view -> goBackToHome());

        // Load user info
        FirebaseDatabase.getInstance().getReference("users").child(receiverId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    tvUsername.setText(snapshot.child("username").getValue(String.class));
                    tvStatus.setText(snapshot.child("status").getValue(String.class));
                });

        buttonSend.setOnClickListener(v -> sendMessage());

        // Load messages and mark as read
        loadMessages();
        markMessagesAsRead();
        listenForSeenMessages();

        // Handle system back press (AndroidX way)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackToHome();
            }
        });


        btnAudioCall.setOnClickListener(view -> {
            startCall(false);
        });

        btnVideoCall.setOnClickListener(view -> {
            startCall(true);
        });


    }

    private void startCall(boolean isVideo) {
        String channelName = "call_" + FirebaseAuth.getInstance().getUid() + "_" + receiverId;
        DatabaseReference callRef = FirebaseDatabase.getInstance().getReference("calls").child(receiverId);

        Map<String, Object> callData = new HashMap<>();
        callData.put("callerId", FirebaseAuth.getInstance().getUid());
        callData.put("channelName", channelName);
        callData.put("isVideo", isVideo);

        callRef.setValue(callData);
        openCallScreen(channelName, isVideo);
    }
    private void openCallScreen(String channelName, boolean isVideo) {
        Intent intent = new Intent(ChatActivity.this, CallActivity.class);
        intent.putExtra("channelName", channelName);
        intent.putExtra("isVideo", isVideo);
        startActivity(intent);
    }

    private void goBackToHome() {
        Intent intent = new Intent(ChatActivity.this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageId);
        messageData.put("senderId", currentUserId);
        messageData.put("receiverId", receiverId);
        messageData.put("message", text);
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("isSeen", false);

        messagesRef.child(messageId).setValue(messageData)
                .addOnSuccessListener(aVoid -> editTextMessage.setText(""));
    }

    private void loadMessages() {
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Message msg = snapshot.getValue(Message.class);
                if (msg != null) {
                    messageList.add(msg);
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                Message updatedMsg = snapshot.getValue(Message.class);
                if (updatedMsg != null) {
                    for (int i = 0; i < messageList.size(); i++) {
                        if (messageList.get(i).getMessageId().equals(updatedMsg.getMessageId())) {
                            messageList.set(i, updatedMsg);
                            messageAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }

            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void markMessagesAsRead() {
        messagesRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot msg : snapshot.getChildren()) {
                String recId = msg.child("receiverId").getValue(String.class);
                if (recId != null && recId.equals(currentUserId)) {
                    msg.getRef().child("isSeen").setValue(true);
                }
            }
        });
    }

    private void listenForSeenMessages() {
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                Message updatedMsg = snapshot.getValue(Message.class);
                if (updatedMsg != null) {
                    for (int i = 0; i < messageList.size(); i++) {
                        if (messageList.get(i).getMessageId().equals(updatedMsg.getMessageId())) {
                            messageList.set(i, updatedMsg);
                            messageAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }
}
