package com.example.trackflow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    // Gunakan nama model yang kamu buat (misal: ActivityItem atau ActivityModel)
    // Di sini saya pakai contoh nama ActivityModel
    private ArrayList<ActivityModel> listActivities = new ArrayList<>();

    public void setData(ArrayList<ActivityModel> items) {
        listActivities.clear();
        listActivities.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menghubungkan dengan layout item_activity (Stitch Theme)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        holder.bind(listActivities.get(position));
    }

    @Override
    public int getItemCount() {
        return listActivities.size();
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvDistance, tvDuration;

        ActivityViewHolder(View itemView) {
            super(itemView);
            // ID ini sesuai dengan yang ada di item_activity.xml kita
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }

        void bind(ActivityModel item) {
            tvTitle.setText(item.getTitle());
            tvDate.setText(item.getDate());
            tvDistance.setText(item.getDistance());
            tvDuration.setText("• " + item.getDuration());
        }
    }
}