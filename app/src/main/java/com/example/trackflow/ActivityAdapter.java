package com.example.trackflow;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private ArrayList<ActivityModel> listActivities = new ArrayList<>();

    public void setData(ArrayList<ActivityModel> items) {
        listActivities.clear();
        listActivities.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityModel activity = listActivities.get(position);
        holder.bind(activity);

        // ---  TEKAN LAMA (LONG CLICK) UNTUK HAPUS DATA ---
        holder.itemView.setOnLongClickListener(v -> {
            Context context = v.getContext();

            new AlertDialog.Builder(context)
                    .setTitle("Hapus Aktivitas")
                    .setMessage("Apakah Anda yakin ingin menghapus riwayat \"" + activity.getTitle() + "\"?")
                    .setPositiveButton("YA, HAPUS", (dialog, which) -> {

                        // 1. Panggil helper database yang sudah kamu buat
                        ActivityHelper activityHelper = ActivityHelper.getInstance(context);
                        activityHelper.open();

                        // 2. Eksekusi hapus menggunakan method deleteById dari kodemu
                        int result = activityHelper.deleteById(String.valueOf(activity.getId()));

                        if (result > 0) {
                            Toast.makeText(context, "Aktivitas berhasil dihapus!", Toast.LENGTH_SHORT).show();

                            // 3. Sinkronisasikan tampilan UI secara real-time
                            int currentPosition = holder.getAdapterPosition();
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                listActivities.remove(currentPosition);
                                notifyItemRemoved(currentPosition);
                                notifyItemRangeChanged(currentPosition, listActivities.size());
                            }
                        } else {
                            Toast.makeText(context, "Gagal menghapus data dari database", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("BATAL", (dialog, which) -> dialog.dismiss())
                    .show();

            return true; // Click event selesai ditangani
        });
    }

    @Override
    public int getItemCount() {
        return listActivities.size();
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvDistance, tvDuration;

        ActivityViewHolder(View itemView) {
            super(itemView);
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