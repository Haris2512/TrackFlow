package com.example.trackflow;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        // --- KLIK ITEM UNTUK DETAIL LATIHAN ---
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            android.content.Intent intent = new android.content.Intent(context, DetailActivity.class);
            intent.putExtra("EXTRA_ID", activity.getId());
            intent.putExtra("EXTRA_TITLE", activity.getTitle());
            intent.putExtra("EXTRA_DISTANCE", activity.getDistance());
            intent.putExtra("EXTRA_DURATION", activity.getDuration());
            intent.putExtra("EXTRA_DATE", activity.getDate());
            intent.putExtra("EXTRA_PATH", activity.getPath());
            context.startActivity(intent);
        });

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
        TextView tvTitle, tvDistance, tvDuration, tvPace;
        TextView tvItemName, tvItemSubtitle, tvItemLocation, tvItemAvatarLetter;
        Button btnLihatKudos;
        MapView mapViewItem;

        ActivityViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvPace = itemView.findViewById(R.id.tvPace);
            
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemSubtitle = itemView.findViewById(R.id.tvItemSubtitle);
            tvItemLocation = itemView.findViewById(R.id.tvItemLocation);
            tvItemAvatarLetter = itemView.findViewById(R.id.tvItemAvatarLetter);
            btnLihatKudos = itemView.findViewById(R.id.btnLihatKudos);
            mapViewItem = itemView.findViewById(R.id.mapViewItem);
        }

        void bind(ActivityModel item) {
            tvTitle.setText(item.getTitle());
            tvDistance.setText(item.getDistance());
            
            // Format durasi (jika format raw, cth: "00:00:18", bersihkan atau biarkan)
            String rawDuration = item.getDuration();
            if (rawDuration.contains(":") && rawDuration.split(":").length == 3) {
                String[] p = rawDuration.split(":");
                try {
                    int sec = Integer.parseInt(p[2]);
                    int min = Integer.parseInt(p[1]);
                    int hr = Integer.parseInt(p[0]);
                    if (hr > 0) {
                        tvDuration.setText(hr + "j " + min + "m");
                    } else if (min > 0) {
                        tvDuration.setText(min + "m " + sec + "s");
                    } else {
                        tvDuration.setText(sec + "d");
                    }
                } catch (Exception e) {
                    tvDuration.setText(rawDuration);
                }
            } else {
                tvDuration.setText(rawDuration);
            }

            // Hitung Pace dinamis
            tvPace.setText(calculatePace(item.getDistance(), item.getDuration()));

            // Ambil data user dari Shared Preferences untuk melengkapi profil card feed
            SharedPreferences prefs = itemView.getContext().getSharedPreferences("TrackFlowPrefs", Context.MODE_PRIVATE);
            String username = prefs.getString("USERNAME", "another rhiez");
            tvItemName.setText(username);
            if (!username.isEmpty()) {
                tvItemAvatarLetter.setText(username.substring(0, 1).toLowerCase());
            }

            // Subtitle
            tvItemSubtitle.setText(item.getDate() + " · TrackFlow");

            // Setup MapView Item (Static Preview)
            if (mapViewItem != null) {
                mapViewItem.setTileSource(TileSourceFactory.MAPNIK);
                mapViewItem.setMultiTouchControls(false); // Non-interaktif

                List<GeoPoint> points = new ArrayList<>();
                String pathStr = item.getPath();
                if (pathStr != null && !pathStr.isEmpty()) {
                    String[] parts = pathStr.split(";");
                    for (String part : parts) {
                        String[] latLng = part.split(",");
                        if (latLng.length == 2) {
                            try {
                                points.add(new GeoPoint(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1])));
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // Fallback rute melingkar organik
                if (points.size() < 2) {
                    points = generateOrganicRoute(item.getId());
                }

                // Draw Path Polyline
                Polyline line = new Polyline();
                line.setColor(Color.parseColor("#FC4C02"));
                line.setWidth(8.0f);
                line.setPoints(points);

                mapViewItem.getOverlays().clear();
                mapViewItem.getOverlays().add(line);

                // Hitung center & zoom
                double sumLat = 0;
                double sumLng = 0;
                for (GeoPoint gp : points) {
                    sumLat += gp.getLatitude();
                    sumLng += gp.getLongitude();
                }
                mapViewItem.getController().setCenter(new GeoPoint(sumLat / points.size(), sumLng / points.size()));
                mapViewItem.getController().setZoom(15.0);
                mapViewItem.invalidate();
            }

            // Aksi tombol Lihat Kudos
            if (btnLihatKudos != null) {
                btnLihatKudos.setOnClickListener(v -> {
                    Context context = v.getContext();
                    android.content.Intent intent = new android.content.Intent(context, DetailActivity.class);
                    intent.putExtra("EXTRA_ID", item.getId());
                    intent.putExtra("EXTRA_TITLE", item.getTitle());
                    intent.putExtra("EXTRA_DISTANCE", item.getDistance());
                    intent.putExtra("EXTRA_DURATION", item.getDuration());
                    intent.putExtra("EXTRA_DATE", item.getDate());
                    intent.putExtra("EXTRA_PATH", item.getPath());
                    context.startActivity(intent);
                });
            }
        }

        private String calculatePace(String distanceStr, String durationStr) {
            try {
                double distance = 0.0;
                String cleanDist = distanceStr.toUpperCase().replace("KM", "").replace(",", ".").trim();
                distance = Double.parseDouble(cleanDist);
                if (distance <= 0) return "--:-- /km";

                double seconds = 0.0;
                String cleanDur = durationStr.toLowerCase().trim();
                if (cleanDur.contains(":")) {
                    String[] parts = cleanDur.split(":");
                    if (parts.length == 3) {
                        seconds = Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
                    } else if (parts.length == 2) {
                        seconds = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                    }
                } else {
                    String numOnly = cleanDur.replaceAll("[^0-9]", "");
                    if (!numOnly.isEmpty()) {
                        int val = Integer.parseInt(numOnly);
                        if (cleanDur.contains("m")) {
                            seconds = val * 60;
                        } else {
                            seconds = val;
                        }
                    }
                }

                if (seconds <= 0) return "--:-- /km";

                double paceMinutesPerKm = (seconds / 60.0) / distance;
                int paceMinutes = (int) paceMinutesPerKm;
                int paceSeconds = (int) ((paceMinutesPerKm - paceMinutes) * 60);
                return String.format(Locale.US, "%d:%02d /km", paceMinutes, paceSeconds);
            } catch (Exception e) {
                return "10:21 /km";
            }
        }

        private List<GeoPoint> generateOrganicRoute(int id) {
            List<GeoPoint> points = new ArrayList<>();
            double centerLat = -5.1345;
            double centerLng = 119.4895;
            
            int routeType = id % 4; 
            if (routeType == 0) {
                // Circular route
                for (int i = 0; i <= 360; i += 20) {
                    double rad = Math.toRadians(i);
                    double r = 0.003 + 0.0005 * Math.sin(rad * 3);
                    points.add(new GeoPoint(centerLat + r * Math.cos(rad), centerLng + r * Math.sin(rad)));
                }
            } else if (routeType == 1) {
                // Figure-8 route
                for (int i = 0; i <= 360; i += 15) {
                    double rad = Math.toRadians(i);
                    double r = 0.004;
                    double x = r * Math.cos(rad);
                    double y = r * Math.sin(rad * 2) / 2.0;
                    points.add(new GeoPoint(centerLat + x, centerLng + y));
                }
            } else if (routeType == 2) {
                // Quad loop
                for (int i = 0; i <= 360; i += 12) {
                    double rad = Math.toRadians(i);
                    double r = 0.0035 * Math.sin(2 * rad);
                    points.add(new GeoPoint(centerLat + r * Math.cos(rad), centerLng + r * Math.sin(rad)));
                }
            } else {
                // Zigzag loop
                for (int i = 0; i <= 360; i += 30) {
                    double rad = Math.toRadians(i);
                    double r = 0.003 + 0.001 * (i % 60 == 0 ? 1 : -1);
                    points.add(new GeoPoint(centerLat + r * Math.cos(rad), centerLng + r * Math.sin(rad)));
                }
            }
            return points;
        }
    }
}