package com.example.trackflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private MapView mapViewDetail;
    private boolean isBookmarked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load config osm
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_detail);

        // Bind Views
        mapViewDetail = findViewById(R.id.mapViewDetail);
        CardView cvBack = findViewById(R.id.cvBack);
        TextView tvAvatarText = findViewById(R.id.tvAvatarText);
        TextView tvUsername = findViewById(R.id.tvUsername);
        TextView tvSubDetails = findViewById(R.id.tvSubDetails);
        TextView tvDetailTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDetailDistance = findViewById(R.id.tvDetailDistance);
        TextView tvDetailDuration = findViewById(R.id.tvDetailDuration);
        TextView tvDetailPace = findViewById(R.id.tvDetailPace);
        Button btnKudosLihat = findViewById(R.id.btnKudosLihat);
        
        CardView cvBookmark = findViewById(R.id.cvBookmark);
        android.widget.ImageView ivBookmarkIcon = findViewById(R.id.ivBookmarkIcon);
        CardView cvMenu = findViewById(R.id.cvMenu);

        // Read extras
        int activityId = getIntent().getIntExtra("EXTRA_ID", 0);
        String title = getIntent().getStringExtra("EXTRA_TITLE");
        String distance = getIntent().getStringExtra("EXTRA_DISTANCE");
        String duration = getIntent().getStringExtra("EXTRA_DURATION");
        String date = getIntent().getStringExtra("EXTRA_DATE");

        // Set Default jika null
        if (title == null || title.isEmpty()) title = "Berlari Siang";
        if (distance == null || distance.isEmpty()) distance = "0,03 km";
        if (duration == null || duration.isEmpty()) duration = "18d";
        if (date == null || date.isEmpty()) date = "Hari ini pukul 12.34";

        // Pastikan format KM konsisten dengan screenshot
        if (!distance.toLowerCase().contains("km")) {
            distance = distance + " km";
        }
        // Ganti titik dengan koma untuk lokalisasi Indonesia seperti di screenshot (0,03 km)
        distance = distance.replace(".", ",");

        // Format pace secara dinamis jika jarak valid
        String paceStr = "10:21 /km";
        try {
            double distVal = Double.parseDouble(distance.replace(" km", "").replace(",", ".").trim());
            if (distVal > 0) {
                // Kalkulasi pace acak rasional antara 5:00 s.d 10:00
                int totalSec = 18;
                if (duration.contains("Menit")) {
                    totalSec = Integer.parseInt(duration.replace(" Menit", "").trim()) * 60;
                } else if (duration.contains("d")) {
                    totalSec = Integer.parseInt(duration.replace("d", "").trim());
                }
                double paceSecondsPerKm = totalSec / distVal;
                int min = (int) (paceSecondsPerKm / 60);
                int sec = (int) (paceSecondsPerKm % 60);
                if (min > 0 && min < 60) {
                    paceStr = String.format(java.util.Locale.getDefault(), "%02d:%02d /km", min, sec);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Tampilkan data ke UI
        tvDetailTitle.setText(title);
        tvDetailDistance.setText(distance);
        tvDetailDuration.setText(duration);
        tvDetailPace.setText(paceStr);

        final String finalDistance = distance;
        final String finalDuration = duration;
        final String finalPaceStr = paceStr;

        // Ambil data SharedPreferences User untuk username & avatar
        SharedPreferences sharedPrefs = getSharedPreferences("TrackFlowPrefs", MODE_PRIVATE);
        String savedName = sharedPrefs.getString("USERNAME", "another rhiez");
        tvUsername.setText(savedName);
        if (savedName.length() > 0) {
            tvAvatarText.setText(savedName.substring(0, 1).toLowerCase());
        }

        tvSubDetails.setText(date + " · Tamalanrea Indah, South Sulawesi");

        // Back button finish
        if (cvBack != null) {
            cvBack.setOnClickListener(v -> finish());
        }

        btnKudosLihat.setOnClickListener(v -> finish());

        // Bookmark Click Toggle
        if (cvBookmark != null && ivBookmarkIcon != null) {
            cvBookmark.setOnClickListener(v -> {
                isBookmarked = !isBookmarked;
                if (isBookmarked) {
                    ivBookmarkIcon.setColorFilter(Color.parseColor("#FC4C02"));
                    android.widget.Toast.makeText(DetailActivity.this, "Ditambahkan ke Favorit!", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    ivBookmarkIcon.setColorFilter(Color.WHITE);
                    android.widget.Toast.makeText(DetailActivity.this, "Dihapus dari Favorit!", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Menu Click (Bagikan & Hapus)
        if (cvMenu != null) {
            cvMenu.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(DetailActivity.this, cvMenu);
                popup.getMenu().add("Bagikan Hasil Lari");
                popup.getMenu().add("Hapus");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Bagikan Hasil Lari")) {
                        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Aktivitas Lari TrackFlow");
                        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hasil Lari TrackFlow saya: Jarak " + finalDistance + ", Durasi " + finalDuration + ", Pace " + finalPaceStr + ". Ayo capai target lari Anda bersama TrackFlow!");
                        startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan"));
                        return true;
                    } else if (item.getTitle().equals("Hapus")) {
                        new androidx.appcompat.app.AlertDialog.Builder(DetailActivity.this)
                            .setTitle("Hapus Aktivitas")
                            .setMessage("Apakah Anda yakin ingin menghapus aktivitas ini?")
                            .setPositiveButton("YA, HAPUS", (dialog, which) -> {
                                ActivityHelper helper = ActivityHelper.getInstance(DetailActivity.this);
                                helper.open();
                                if (activityId != 0) {
                                    helper.deleteById(String.valueOf(activityId));
                                }
                                android.widget.Toast.makeText(DetailActivity.this, "Aktivitas berhasil dihapus", android.widget.Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .setNegativeButton("BATAL", (dialog, which) -> dialog.dismiss())
                            .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        // Inisialisasi Map dan gambar lintasan oranye dinamis (Screenshot 1)
        if (mapViewDetail != null) {
            mapViewDetail.setTileSource(TileSourceFactory.MAPNIK);
            mapViewDetail.setMultiTouchControls(true);

            IMapController controller = mapViewDetail.getController();

            // Ambil koordinat GPS dari Intent jika ada
            String pathStr = getIntent().getStringExtra("EXTRA_PATH");
            List<GeoPoint> points = new ArrayList<>();
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

            boolean isFallback = false;
            // Fallback ke rute melingkar alami/estetik jika data GPS kosong/kurang
            if (points.size() < 2) {
                isFallback = true;
                points = generateOrganicRoute(activityId);
            }

            if (isFallback) {
                controller.setZoom(15.5);
            } else {
                controller.setZoom(18.5);
            }

            GeoPoint pStart = points.get(0);
            GeoPoint pEnd = points.get(points.size() - 1);

            // Pusatkan peta pada titik tengah rute secara dinamis
            double sumLat = 0;
            double sumLng = 0;
            for (GeoPoint gp : points) {
                sumLat += gp.getLatitude();
                sumLng += gp.getLongitude();
            }
            controller.setCenter(new GeoPoint(sumLat / points.size(), sumLng / points.size()));

            // Polyline oranye tebal khas Strava
            Polyline routeLine = new Polyline();
            routeLine.setColor(Color.parseColor("#FC4C02"));
            routeLine.setWidth(12.0f);
            routeLine.setPoints(points);
            mapViewDetail.getOverlayManager().add(routeLine);

            // Marker titik awal (Hijau terang bulat)
            Marker startMarker = new Marker(mapViewDetail);
            startMarker.setPosition(pStart);
            startMarker.setIcon(new BitmapDrawable(getResources(), createMarkerDot(Color.parseColor("#7CFC00"), 36)));
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            startMarker.setTitle("Titik Mulai");
            mapViewDetail.getOverlays().add(startMarker);

            // Marker titik akhir (Bendera kotak catur bulat kustom)
            Marker endMarker = new Marker(mapViewDetail);
            endMarker.setPosition(pEnd);
            endMarker.setIcon(new BitmapDrawable(getResources(), createCheckeredMarkerDot(36)));
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            endMarker.setTitle("Titik Selesai");
            mapViewDetail.getOverlays().add(endMarker);
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

    // Helper untuk menggambar marker bulat polos
    private Bitmap createMarkerDot(int color, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.6f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        return bitmap;
    }

    // Helper untuk menggambar penanda kotak catur (checkered flag)
    private Bitmap createCheckeredMarkerDot(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2.5f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Gambar pola kotak hitam di dalam
        float radius = size / 2.6f;
        float cx = size / 2f;
        float cy = size / 2f;

        // Clip area bulat kecil
        canvas.save();
        android.graphics.Path path = new android.graphics.Path();
        path.addCircle(cx, cy, radius, android.graphics.Path.Direction.CW);
        canvas.clipPath(path);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        
        // Kotak kiri atas dan kanan bawah hitam
        canvas.drawRect(cx - radius, cy - radius, cx, cy, paint);
        canvas.drawRect(cx, cy, cx + radius, cy + radius, paint);

        canvas.restore();
        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapViewDetail != null) mapViewDetail.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewDetail != null) mapViewDetail.onPause();
    }
}
