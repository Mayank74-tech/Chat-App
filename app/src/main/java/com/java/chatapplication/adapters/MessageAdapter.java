package com.java.chatapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.java.chatapplication.R;
import com.java.chatapplication.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private Context context;
    private List<Message> messages;
    private String currentUserId;
    private String currentUserImage;

    public MessageAdapter(Context context, List<Message> messages, String currentUserId, String currentUserImage) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.currentUserImage = currentUserImage;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_send, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String formattedTime = timeFormat.format(message.getTimestamp());

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.messageText.setText(message.getMessage());
            sentHolder.timeText.setText(formattedTime);

            // Show double tick if message is delivered/read
            sentHolder.messageText.setText(message.getMessage());
            sentHolder.timeText.setText(formattedTime);

            if (message.isSeen()) {
                sentHolder.messageStatus.setVisibility(View.VISIBLE);
            } else {
                sentHolder.messageStatus.setVisibility(View.GONE);
            }


            // Load current user's profile image if available
            if (currentUserImage != null && !currentUserImage.isEmpty()) {
                Glide.with(context)
                        .load(currentUserImage)
                        .placeholder(R.drawable.default_avatar)
                        .circleCrop()
                        .into(sentHolder.profileImage);
            }
        } else {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            receivedHolder.messageText.setText(message.getMessage());
            receivedHolder.timeText.setText(formattedTime);
            receivedHolder.senderName.setText(message.getSenderName());

            if (message.getSenderImage() != null && !message.getSenderImage().isEmpty()) {
                Glide.with(context)
                        .load(message.getSenderImage())
                        .placeholder(R.drawable.default_avatar)
                        .circleCrop()
                        .into(receivedHolder.profileImage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    // ViewHolder for sent messages
    public static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView profileImage,messageStatus;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tvSentMessage);
            timeText = itemView.findViewById(R.id.tvMessageTime);
            profileImage = itemView.findViewById(R.id.ivProfile);
            messageStatus = itemView.findViewById(R.id.image_status_tick);
        }
    }

    // ViewHolder for received messages
    public static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, senderName;
        ImageView profileImage,messageStatus;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tvReceivedMessage);
            timeText = itemView.findViewById(R.id.tvMessageTime);
            senderName = itemView.findViewById(R.id.tvSenderName);
            profileImage = itemView.findViewById(R.id.ivProfile);

        }
    }
}