package com.java.chatapplication.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.java.chatapplication.ChatActivity;
import com.java.chatapplication.R;
import com.java.chatapplication.models.User;

import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final Context context;
    private final List<User> userList;
    private Map<String, Integer> unreadCountMap;
    private final boolean showLastMessage;

    public UserAdapter(Context context, List<User> userList, Map<String, Integer> unreadCountMap, boolean showLastMessage) {
        this.context = context;
        this.userList = userList;
        this.unreadCountMap = unreadCountMap;
        this.showLastMessage = showLastMessage;
    }

    public void setUnreadCountMap(Map<String, Integer> unreadCountMap) {
        this.unreadCountMap = unreadCountMap;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.username.setText(user.getUsername());

        if (showLastMessage) {
            holder.status.setVisibility(View.VISIBLE);
            holder.status.setText(user.getStatus() != null ? user.getStatus() : "");
        } else {
            holder.status.setVisibility(View.GONE);
        }

        // Show unread badge if unread count > 0
        Integer unreadCount = unreadCountMap.get(user.getUid());
        if (unreadCount != null && unreadCount > 0) {
            holder.unreadBadge.setText(String.valueOf(unreadCount));
            holder.unreadBadge.setVisibility(View.VISIBLE);
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.default_avatar)
                .into(holder.profileImage);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("userId", user.getUid());
            intent.putExtra("username", user.getUsername());
            intent.putExtra("profileImage", user.getProfileImageUrl());
            intent.putExtra("status", user.getStatus());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username, status, unreadBadge;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.image_profile);
            username = itemView.findViewById(R.id.text_username);
            status = itemView.findViewById(R.id.text_status);
            unreadBadge = itemView.findViewById(R.id.text_unread_badge); // Make sure this exists in your layout
        }
    }
}
