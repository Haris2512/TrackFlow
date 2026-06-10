package com.example.trackflow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private android.content.SharedPreferences sharedPreferences;

    public UserAdapter(List<User> userList, android.content.SharedPreferences sharedPreferences) {
        this.userList = userList;
        this.sharedPreferences = sharedPreferences;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvName.setText(user.getFirstName() + " " + user.getLastName());
        
        // Data Dummy Aktivitas Olahraga
        String[] titles = {"Lari Pagi Santai", "Latihan Interval Sore", "Long Run Akhir Pekan", "Lari Cepat 5K", "Lari Pemulihan", "Eksplorasi Rute Baru"};
        holder.tvActivityTitle.setText(titles[position % titles.length]);
        holder.tvTime.setText("Hari ini pada 0" + (6 + position % 4) + ":00");
        holder.tvDistance.setText(String.format(java.util.Locale.US, "%.2f km", 3.0 + position * 1.5));
        holder.tvPace.setText("0" + (5 + position % 3) + ":" + (10 + position * 5) + " /km");
        holder.tvDuration.setText((20 + position * 6) + "m " + (15 + position * 2) + "s");
        
        int initialKudos = position * 2 + 1;
        holder.tvKudosCount.setText(String.valueOf(initialKudos));
        holder.ivKudos.setColorFilter(null); // Reset color
        
        holder.btnKudos.setOnClickListener(v -> {
            holder.ivKudos.setColorFilter(android.graphics.Color.parseColor("#FC4C02"));
            holder.tvKudosCount.setText(String.valueOf(initialKudos + 1));
            android.widget.Toast.makeText(v.getContext(), "Anda memberikan Kudos kepada " + user.getFirstName() + "!", android.widget.Toast.LENGTH_SHORT).show();
        });

        holder.btnComment.setOnClickListener(v -> {
            android.widget.Toast.makeText(v.getContext(), "Fitur komentar segera hadir!", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Load Follow State from SharedPreferences
        String followKey = "FOLLOW_" + user.getEmail();
        boolean isFollowed = sharedPreferences.getBoolean(followKey, false);
        
        if (isFollowed) {
            holder.btnFollow.setText("Mengikuti");
            holder.btnFollow.setTextColor(android.graphics.Color.GRAY);
        } else {
            holder.btnFollow.setText("Ikuti");
            holder.btnFollow.setTextColor(android.graphics.Color.parseColor("#FC4C02"));
        }

        holder.btnFollow.setOnClickListener(v -> {
            boolean currentlyFollowed = sharedPreferences.getBoolean(followKey, false);
            android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
            
            if (!currentlyFollowed) {
                holder.btnFollow.setText("Mengikuti");
                holder.btnFollow.setTextColor(android.graphics.Color.GRAY);
                editor.putBoolean(followKey, true);
                android.widget.Toast.makeText(v.getContext(), "Anda sekarang mengikuti " + user.getFirstName() + "!", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                holder.btnFollow.setText("Ikuti");
                holder.btnFollow.setTextColor(android.graphics.Color.parseColor("#FC4C02"));
                editor.putBoolean(followKey, false);
                android.widget.Toast.makeText(v.getContext(), "Batal mengikuti " + user.getFirstName() + ".", android.widget.Toast.LENGTH_SHORT).show();
            }
            editor.apply();
        });

        Glide.with(holder.itemView.getContext())
                .load(user.getImage()) // Mengambil 'image' dari DummyJSON
                .circleCrop()
                .into(holder.ivAvatar);

        // Buka Halaman Detail Atlet Saat Kartu Diklik
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), DetailAthleteActivity.class);
            intent.putExtra("EXTRA_NAME", user.getFirstName() + " " + user.getLastName());
            intent.putExtra("EXTRA_IMAGE", user.getImage());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList == null ? 0 : userList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivKudos;
        TextView tvName, tvTime, tvActivityTitle, tvDistance, tvPace, tvDuration, tvKudosCount, btnFollow;
        android.widget.LinearLayout btnKudos, btnComment;

        UserViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvActivityTitle = itemView.findViewById(R.id.tvActivityTitle);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvPace = itemView.findViewById(R.id.tvPace);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ivKudos = itemView.findViewById(R.id.ivKudos);
            tvKudosCount = itemView.findViewById(R.id.tvKudosCount);
            btnKudos = itemView.findViewById(R.id.btnKudos);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }
    }
}