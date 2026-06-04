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

        // Read extras
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

        // Inisialisasi Map dan gambar lintasan oranye dinamis (Screenshot 1)
        if (mapViewDetail != null) {
            mapViewDetail.setTileSource(TileSourceFactory.MAPNIK);
            mapViewDetail.setMultiTouchControls(true);

            IMapController controller = mapViewDetail.getController();
            controller.setZoom(19.0);

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

            // Fallback ke rute melingkar tiruan (closed loop) yang estetik jika data GPS kosong/kurang
            if (points.size() < 2) {
                points.clear();
                double centerLat = -5.147665;
                double centerLng = 119.432731;
                double r = 0.0015; // Radius sekitar 150 meter
                for (int i = 0; i <= 8; i++) {
                    double angle = i * (2 * Math.PI / 8);
                    points.add(new GeoPoint(centerLat + r * Math.sin(angle), centerLng + r * Math.cos(angle)));
                }
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
