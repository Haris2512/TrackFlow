package com.example.trackflow;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Locale;

public class RecordFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;

    // View Map View
    private ConstraintLayout clMapOverlay;
    private CardView cvStopwatch;
    private TextView tvStopwatch;
    private FloatingActionButton fabPlay;
    private CardView cvCollapse;

    // View Dashboard View (Stats)
    private LinearLayout llDashboardView;
    private LinearLayout llDashStatusBar;
    private TextView tvDashStopwatch;
    private TextView tvDashSplitVal;
    private TextView tvDashDistanceVal;
    private ImageView ivDashCollapse;

    // Bottom Sheet Control Views
    private ConstraintLayout clActionRow;
    private Button btnJeda;
    private LinearLayout llPauseButtons;
    private Button btnLanjutkan;
    private Button btnSelesaikan;

    private boolean isRunning = false;
    private int seconds = 0;
    private double currentDistance = 0.0;
    private Handler handler = new Handler(Looper.getMainLooper());

    public RecordFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi Map
        mapView = view.findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        IMapController mapController = mapView.getController();
        mapController.setZoom(19.0);
        GeoPoint defaultLocation = new GeoPoint(-5.147665, 119.432731); // Makassar
        mapController.setCenter(defaultLocation);

        // Inisialisasi widget UI Map
        clMapOverlay = view.findViewById(R.id.clMapOverlay);
        cvStopwatch = view.findViewById(R.id.cvStopwatch);
        tvStopwatch = view.findViewById(R.id.tvStopwatch);
        fabPlay = view.findViewById(R.id.fabPlay);
        cvCollapse = view.findViewById(R.id.cvCollapse);

        // Inisialisasi widget UI Dashboard
        llDashboardView = view.findViewById(R.id.llDashboardView);
        llDashStatusBar = view.findViewById(R.id.llDashStatusBar);
        tvDashStopwatch = view.findViewById(R.id.tvDashStopwatch);
        tvDashSplitVal = view.findViewById(R.id.tvDashSplitVal);
        tvDashDistanceVal = view.findViewById(R.id.tvDashDistanceVal);
        ivDashCollapse = view.findViewById(R.id.ivDashCollapse);

        // Inisialisasi widget Bottom Sheet
        clActionRow = view.findViewById(R.id.clActionRow);
        btnJeda = view.findViewById(R.id.btnJeda);
        llPauseButtons = view.findViewById(R.id.llPauseButtons);
        btnLanjutkan = view.findViewById(R.id.btnLanjutkan);
        btnSelesaikan = view.findViewById(R.id.btnSelesaikan);

        // Awal mula: Tampilkan peta, sembunyikan dashboard
        llDashboardView.setVisibility(View.GONE);
        cvStopwatch.setVisibility(View.GONE);

        // Tombol collapse kembali ke halaman sebelumnya
        cvCollapse.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Tapping floating stopwatch akan membawa ke Dashboard View
        cvStopwatch.setOnClickListener(v -> {
            clMapOverlay.setVisibility(View.GONE);
            llDashboardView.setVisibility(View.VISIBLE);
        });

        // Tombol ciutkan di Dashboard View membawa kembali ke Map View
        ivDashCollapse.setOnClickListener(v -> {
            llDashboardView.setVisibility(View.GONE);
            clMapOverlay.setVisibility(View.VISIBLE);
            if (isRunning || seconds > 0) {
                cvStopwatch.setVisibility(View.VISIBLE);
            }
        });

        // 1. Aksi ketika tombol bulat play ditekan (Memulai latihan pertama kali)
        fabPlay.setOnClickListener(v -> {
            startTracking();
        });

        // 2. Aksi tombol Jeda (Pause)
        btnJeda.setOnClickListener(v -> {
            pauseTracking();
        });

        // 3. Aksi tombol Lanjutkan (Resume)
        btnLanjutkan.setOnClickListener(v -> {
            resumeTracking();
        });

        // 4. Aksi tombol Selesaikan (Finish) -> Menuju FormActivity
        btnSelesaikan.setOnClickListener(v -> {
            finishTracking();
        });

        // Cek permission GPS
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            enableMyLocation();
        }
    }

    private void startTracking() {
        isRunning = true;
        seconds = 0;
        currentDistance = 0.0;

        tvStopwatch.setText("00:00:00");
        tvDashStopwatch.setText("00:00:00");
        tvDashDistanceVal.setText("0,00");
        tvDashSplitVal.setText("---");

        // Transisi Visual
        clActionRow.setVisibility(View.GONE);
        btnJeda.setVisibility(View.VISIBLE);
        llPauseButtons.setVisibility(View.GONE);

        // Tampilkan Dashboard View (Screenshot 2)
        clMapOverlay.setVisibility(View.GONE);
        llDashboardView.setVisibility(View.VISIBLE);
        llDashStatusBar.setVisibility(View.GONE);

        runStopwatch();
    }

    private void pauseTracking() {
        isRunning = false;

        // Transisi Visual
        clActionRow.setVisibility(View.GONE);
        btnJeda.setVisibility(View.GONE);
        llPauseButtons.setVisibility(View.VISIBLE);

        // Tampilkan Header Kuning "Berhenti" di Dashboard (Screenshot 4)
        llDashStatusBar.setVisibility(View.VISIBLE);
    }

    private void resumeTracking() {
        isRunning = true;

        // Transisi Visual
        clActionRow.setVisibility(View.GONE);
        btnJeda.setVisibility(View.VISIBLE);
        llPauseButtons.setVisibility(View.GONE);
        llDashStatusBar.setVisibility(View.GONE);

        runStopwatch();
    }

    private void finishTracking() {
        isRunning = false;

        int totalMin = seconds / 60; // Konversi total detik berjalan ke menit
        String formattedDurationForForm;

        if (totalMin <= 0) {
            formattedDurationForForm = seconds + "d"; // Mengikuti "18d" di Screenshot 1 (d = detik)
        } else if (totalMin < 60) {
            formattedDurationForForm = totalMin + " Menit";
        } else {
            int hours = totalMin / 60;
            int remainingMinutes = totalMin % 60;

            if (remainingMinutes == 0) {
                formattedDurationForForm = hours + " Jam";
            } else {
                formattedDurationForForm = hours + " Jam " + remainingMinutes + " Menit";
            }
        }

        // Hitung jarak terformat
        String formattedDistance = String.format(Locale.getDefault(), "%.2f", currentDistance);

        // Pindah ke FormActivity dan bawa durasi pintar serta jarak
        Intent intent = new Intent(requireContext(), FormActivity.class);
        intent.putExtra("EXTRA_DURATION", formattedDurationForForm);
        intent.putExtra("EXTRA_DISTANCE", formattedDistance);
        startActivity(intent);

        // Reset komponen setelah data dikirim
        resetState();
    }

    private void resetState() {
        isRunning = false;
        seconds = 0;
        currentDistance = 0.0;

        tvStopwatch.setText("00:00:00");
        tvDashStopwatch.setText("00:00:00");
        tvDashDistanceVal.setText("0,00");
        tvDashSplitVal.setText("---");

        clActionRow.setVisibility(View.VISIBLE);
        btnJeda.setVisibility(View.GONE);
        llPauseButtons.setVisibility(View.GONE);

        llDashboardView.setVisibility(View.GONE);
        llDashStatusBar.setVisibility(View.GONE);
        clMapOverlay.setVisibility(View.VISIBLE);
        cvStopwatch.setVisibility(View.GONE);
    }

    private void enableMyLocation() {
        if (getContext() == null || mapView == null) return;

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);

        Bitmap blueDot = createSleekBlueDot();
        locationOverlay.setPersonIcon(blueDot);
        locationOverlay.setDirectionArrow(blueDot, blueDot);
        locationOverlay.setPersonHotspot(blueDot.getWidth() / 2f, blueDot.getHeight() / 2f);
        locationOverlay.setDrawAccuracyEnabled(true);

        locationOverlay.enableMyLocation();

        locationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    GeoPoint myLoc = locationOverlay.getMyLocation();
                    if (myLoc != null && mapView != null) {
                        mapView.getController().animateTo(myLoc);
                        mapView.getController().setZoom(20.0);
                    }
                });
            }
        });

        mapView.getOverlays().add(locationOverlay);
    }

    private Bitmap createSleekBlueDot() {
        int size = 44;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.3f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#40000000"));
        paint.setStrokeWidth(1.5f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.3f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1A73E8"));
        canvas.drawCircle(size / 2f, size / 2f, size / 3.8f, paint);

        return bitmap;
    }

    // Logika stopwatch + update dynamic stats
    private void runStopwatch() {
        handler.removeCallbacksAndMessages(null);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // Update stopwatch di Map dan di Dashboard
                    String timeFormatted = formatTime(seconds);
                    tvStopwatch.setText(timeFormatted);
                    tvDashStopwatch.setText(timeFormatted);

                    // Update jarak berjalan palsu untuk demonstrasi (0.0015 KM per detik)
                    currentDistance = seconds * 0.00167; // contoh: 18 detik = ~0.03 KM
                    tvDashDistanceVal.setText(String.format(Locale.getDefault(), "%.2f", currentDistance));

                    // Update split pace
                    if (seconds > 2) {
                        double paceInSeconds = (double) seconds / currentDistance;
                        int paceMin = (int) (paceInSeconds / 60);
                        int paceSec = (int) (paceInSeconds % 60);
                        tvDashSplitVal.setText(String.format(Locale.getDefault(), "%02d:%02d", paceMin, paceSec));
                    } else {
                        tvDashSplitVal.setText("---");
                    }

                    seconds++;
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
    }
}