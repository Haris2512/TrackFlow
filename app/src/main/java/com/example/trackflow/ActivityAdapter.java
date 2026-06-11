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

        // Klik item untuk buka detail aktivitas
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

        // Tekan lama untuk hapus data
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

            // Nama user dari SharedPreferences
            SharedPreferences prefs = itemView.getContext().getSharedPreferences("TrackFlowPrefs", Context.MODE_PRIVATE);
            String username = prefs.getString("USERNAME", "another rhiez");
            tvItemName.setText(username);
            if (!username.isEmpty()) {
                tvItemAvatarLetter.setText(username.substring(0, 1).toLowerCase());
            }

            // Subtitle
            tvItemSubtitle.setText(item.getDate() + " · TrackFlow");

            // Location
            if (tvItemLocation != null) {
                tvItemLocation.setText("Memuat lokasi...");
                String pathStrLocation = item.getPath();
                if (pathStrLocation != null && !pathStrLocation.isEmpty()) {
                    String[] parts = pathStrLocation.split(";");
                    if (parts.length > 0) {
                        String[] latLng = parts[0].split(",");
                        if (latLng.length == 2) {
                            try {
                                double lat = Double.parseDouble(latLng[0]);
                                double lng = Double.parseDouble(latLng[1]);
                                new Thread(() -> {
                                    try {
                                        android.location.Geocoder gc = new android.location.Geocoder(itemView.getContext(), java.util.Locale.getDefault());
                                        java.util.List<android.location.Address> list = gc.getFromLocation(lat, lng, 1);
                                        if (list != null && !list.isEmpty()) {
                                            android.location.Address a = list.get(0);
                                            String addressLine = a.getAddressLine(0);
                                            String tempLoc = "";
                                            if (addressLine != null && !addressLine.isEmpty()) {
                                                String[] addressParts = addressLine.split(",");
                                                tempLoc = addressParts[0].trim() + (addressParts.length > 1 ? ", " + addressParts[1].trim() : "");
                                            }
                                            final String finalLoc = tempLoc.isEmpty() ? "Lokasi Tidak Diketahui" : tempLoc;
                                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> tvItemLocation.setText(finalLoc));
                                        } else {
                                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> tvItemLocation.setText("Makassar, South Sulawesi"));
                                        }
                                    } catch (Exception e) {
                                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> tvItemLocation.setText("Makassar, South Sulawesi"));
                                    }
                                }).start();
                            } catch (Exception ignored) {
                                tvItemLocation.setText("Makassar, South Sulawesi");
                            }
                        } else {
                            tvItemLocation.setText("Makassar, South Sulawesi");
                        }
                    } else {
                        tvItemLocation.setText("Makassar, South Sulawesi");
                    }
                } else {
                    tvItemLocation.setText("Makassar, South Sulawesi");
                }
            }

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

            // Tombol Lihat Kudos juga mengarah ke halaman detail yang sama
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

        // Hasilkan rute organik unik berdasarkan id sebagai seed
        // Ini hanya untuk preview visual di kartu riwayat, bukan rute GPS asli
        private List<GeoPoint> generateOrganicRoute(int id) {
            List<GeoPoint> points = new ArrayList<>();
            double centerLat = -5.1345;
            double centerLng = 119.4895;
            
            double seed = (id * 17) % 100 / 100.0;
            double scale = 0.003 + (seed * 0.0015);
            
            int numSteps = 24;
            for (int i = 0; i < numSteps; i++) {
                double angle = (2.0 * Math.PI * i) / numSteps;
                double r = scale * (1.0 + 0.25 * Math.sin(angle * 3) + 0.15 * Math.cos(angle * 5) + 0.08 * Math.sin(angle * 7));
                double rotatedAngle = angle + (seed * Math.PI / 2.0);
                
                double latOffset = r * Math.cos(rotatedAngle);
                double lngOffset = r * Math.sin(rotatedAngle) * 1.2;
                points.add(new GeoPoint(centerLat + latOffset, centerLng + lngOffset));
            }
            points.add(points.get(0)); // Tutup loop
            return points;
        }
    }
}