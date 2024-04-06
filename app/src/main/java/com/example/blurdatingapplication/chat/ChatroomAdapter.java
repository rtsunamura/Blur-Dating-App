package com.example.blurdatingapplication.chat;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.blurdatingapplication.R;
import com.example.blurdatingapplication.data.UserData;
import com.example.blurdatingapplication.utils.FireBaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatroomAdapter extends FirestoreRecyclerAdapter<ChatroomModel, ChatroomAdapter.ChatroomViewHolder> {
    Context context;
    UserData otherUser;
    private OnChatroomClickListener chatroomClickListener;
    public ChatroomAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context, OnChatroomClickListener chatroomClickListener) {
        super(options);
        this.context = context;
        this.chatroomClickListener = chatroomClickListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomViewHolder holder, int position, @NonNull ChatroomModel model) {
        List<String> userIds = model.getUserIds();

        String otherUserId = (userIds.get(0).equals(FireBaseUtil.getUserID())) ? userIds.get(1) : userIds.get(0);

        //holder.otherUserTextView.setText(otherUserId);

        // Fetch userdata for the other user
        FirebaseFirestore.getInstance().collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        if (username != null) {
                            holder.otherUserTextView.setText(username);
                        } else {
                            //Log.e(TAG, "Username not found for user ID: " + otherUserId);
                            // Handle the error, set a default username or show an error message
                        }
                    } else {
                        //Log.e(TAG, "User not found for user ID: " + otherUserId);
                        // Handle the error, set a default username or show an error message
                    }
                })
                .addOnFailureListener(e -> {
                    //Log.e(TAG, "Error fetching username: " + e.getMessage());
                    // Handle the error, set a default username or show an error message
                });



        holder.lastMessageText.setText(model.getLastM()); // Adjust as per your logic

        // holder.unreadCountText.setText(String.valueOf(model.getUnreadCount())); // Adjust as per your logic
        holder.itemView.setOnClickListener(view -> {
            if (chatroomClickListener != null) {
                chatroomClickListener.onChatroomClick(model);
            }
        });
    }

    @NonNull
    @Override
    public ChatroomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chatroom, parent, false);
        return new ChatroomViewHolder(view);
    }

    static class ChatroomViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView otherUserTextView;
        TextView lastMessageText;
       // TextView timestampText;
        public ChatroomViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImage = itemView.findViewById(R.id.profileImageView);
            otherUserTextView = itemView.findViewById(R.id.otherUserTextView);
            lastMessageText = itemView.findViewById(R.id.lastMessageTextView);
            //timestampText = itemView.findViewById(R.id.timestamp_text);

        }
    }

    // Interface for click Listener
    public interface OnChatroomClickListener {
        void onChatroomClick(ChatroomModel chatroom);
    }
}