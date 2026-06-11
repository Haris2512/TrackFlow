package com.example.trackflow;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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
import androidx.appcompat.app.AlertDialog;
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
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Locale;

public class RecordFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;

    // Overlay peta & kontrol saat aktif merekam
    private ConstraintLayout clMapOverlay;
    private CardView cvStopwatch;
    private TextView tvStopwatch;
    private FloatingActionButton fabPlay;
    private CardView cvCollapse;

    // Tampilan dashboard statistik (muncul saat rekaman berjalan)
    private LinearLayout llDashboardView;
    private LinearLayout llDashStatusBar;
    private TextView tvDashStopwatch;
    private TextView tvDashSplitVal;
    private TextView tvDashDistanceVal;
    private ImageView ivDashCollapse;

    // Tombol kontrol lari (Jeda, Lanjutkan, Selesaikan)
    private ConstraintLayout clActionRow;
    private Button btnJeda;
    private LinearLayout llPauseButtons;
    private Button btnLanjutkan;
    private Button btnSelesaikan;

    // Pilihan jenis olahraga
    private LinearLayout llRun;
    private ImageView ivSportIcon;
    private TextView tvSportName;
    private String selectedSport = "Berlari";
    private int selectedSportIconRes = R.drawable.ic_shoe;

    // Koneksi ke TrackingService yang berjalan di background
    private TrackingService trackingService;
    private boolean isBound = false;
    private Polyline livePolyline;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
            trackingService = binder.getService();
            isBound = true;
            syncUIWithService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    private final BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(TrackingService.BROADCAST_TICK)) {
                    if (trackingService != null) {
                        updateDashboardStats(trackingService.getSeconds(), trackingService.getCurrentDistance());
                    }
                } else if (intent.getAction().equals(TrackingService.BROADCAST_LOCATION)) {
                    double lat = intent.getDoubleExtra("lat", 0);
                    double lon = intent.getDoubleExtra("lon", 0);
                    if (livePolyline != null) {
                        livePolyline.addPoint(new GeoPoint(lat, lon));
                        if (mapView != null)
                            mapView.invalidate();
                    }
                }
            }
        }
    };

    public RecordFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
        java.io.File osmdroidBasePath = new java.io.File(ctx.getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmdroidBasePath);
        java.io.File osmdroidTileCache = new java.io.File(osmdroidBasePath, "tiles");
        Configuration.getInstance().setOsmdroidTileCache(osmdroidTileCache);
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupMap();
        setupListeners();
        checkPermissions();
    }

    private void initViews(View view) {
        clMapOverlay = view.findViewById(R.id.clMapOverlay);
        cvStopwatch = view.findViewById(R.id.cvStopwatch);
        tvStopwatch = view.findViewById(R.id.tvStopwatch);
        fabPlay = view.findViewById(R.id.fabPlay);
        cvCollapse = view.findViewById(R.id.cvCollapse);

        llDashboardView = view.findViewById(R.id.llDashboardView);
        llDashStatusBar = view.findViewById(R.id.llDashStatusBar);
        tvDashStopwatch = view.findViewById(R.id.tvDashStopwatch);
        tvDashSplitVal = view.findViewById(R.id.tvDashSplitVal);
        tvDashDistanceVal = view.findViewById(R.id.tvDashDistanceVal);
        ivDashCollapse = view.findViewById(R.id.ivDashCollapse);

        clActionRow = view.findViewById(R.id.clActionRow);
        btnJeda = view.findViewById(R.id.btnJeda);
        llPauseButtons = view.findViewById(R.id.llPauseButtons);
        btnLanjutkan = view.findViewById(R.id.btnLanjutkan);
        btnSelesaikan = view.findViewById(R.id.btnSelesaikan);

        llRun = view.findViewById(R.id.llRun);
        ivSportIcon = view.findViewById(R.id.ivSportIcon);
        tvSportName = view.findViewById(R.id.tvSportName);

        llDashboardView.setVisibility(View.GONE);
        cvStopwatch.setVisibility(View.GONE);
    }

    private void setupMap() {
        mapView = requireView().findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        IMapController mapController = mapView.getController();
        mapController.setZoom(19.0);
        mapController.setCenter(new GeoPoint(-5.147665, 119.432731));

        livePolyline = new Polyline();
        livePolyline.setColor(Color.parseColor("#FC4C02"));
        livePolyline.setWidth(12.0f);
        mapView.getOverlays().add(livePolyline);
    }

    private void setupListeners() {
        cvCollapse.setOnClickListener(v -> {
            if (getActivity() != null)
                getActivity().onBackPressed();
        });

        if (llRun != null) {
            llRun.setOnClickListener(v -> showSportPickerDialog());
        }

        cvStopwatch.setOnClickListener(v -> {
            clMapOverlay.setVisibility(View.GONE);
            llDashboardView.setVisibility(View.VISIBLE);
        });

        ivDashCollapse.setOnClickListener(v -> {
            llDashboardView.setVisibility(View.GONE);
            clMapOverlay.setVisibility(View.VISIBLE);
            if (trackingService != null && (trackingService.isTracking() || trackingService.getSeconds() > 0)) {
                cvStopwatch.setVisibility(View.VISIBLE);
            }
        });

        fabPlay.setOnClickListener(v -> startTracking());
        btnJeda.setOnClickListener(v -> pauseTracking());
        btnLanjutkan.setOnClickListener(v -> resumeTracking());
        btnSelesaikan.setOnClickListener(v -> finishTracking());
    }

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS };
        } else {
            permissions = new String[] { Manifest.permission.ACCESS_FINE_LOCATION };
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(requireActivity(), permissions, 1);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (getContext() == null || mapView == null)
            return;
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);

        Bitmap blueDot = createSleekBlueDot();
        locationOverlay.setPersonIcon(blueDot);
        locationOverlay.setDirectionArrow(blueDot, blueDot);
        locationOverlay.setPersonHotspot(blueDot.getWidth() / 2f, blueDot.getHeight() / 2f);
        locationOverlay.setDrawAccuracyEnabled(true);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation(); // Peta otomatis ikuti posisi user

        locationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    GeoPoint myLoc = locationOverlay.getMyLocation();
                    if (myLoc != null && mapView != null) {
                        mapView.getController().animateTo(myLoc);
                    }
                });
            }
        });
        mapView.getOverlays().add(locationOverlay);
    }

    private void sendServiceAction(String action) {
        Intent serviceIntent = new Intent(requireContext(), TrackingService.class);
        serviceIntent.setAction(action);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
    }

    private void startTracking() {
        android.location.LocationManager lm = (android.location.LocationManager) requireContext()
                .getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
        }

        // Tolak mulai jika GPS mati, beri tahu user dengan jelas
        if (!gpsEnabled) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Sinyal GPS Lemah / Mati")
                    .setMessage(
                            "Kami tidak dapat mendeteksi sinyal GPS. Pastikan fitur Lokasi di ponsel Anda sudah menyala dan Anda berada di luar ruangan agar pelacakan rute berjalan akurat.")
                    .setPositiveButton("Mengerti", null)
                    .show();
            return;
        }

        sendServiceAction(TrackingService.ACTION_START);

        tvStopwatch.setText("00:00:00");
        tvDashStopwatch.setText("00:00:00");
        tvDashDistanceVal.setText("0,00");
        tvDashSplitVal.setText("---");

        clActionRow.setVisibility(View.GONE);
        btnJeda.setVisibility(View.VISIBLE);
        llPauseButtons.setVisibility(View.GONE);

        clMapOverlay.setVisibility(View.GONE);
        llDashboardView.setVisibility(View.VISIBLE);
        llDashStatusBar.setVisibility(View.GONE);

        livePolyline.setPoints(new ArrayList<>());
    }

    private void pauseTracking() {
        sendServiceAction(TrackingService.ACTION_PAUSE);
        clActionRow.setVisibility(View.GONE);
        btnJeda.setVisibility(View.GONE);
        llPauseButtons.setVisibility(View.VISIBLE);
        llDashStatusBar.setVisibility(View.VISIBLE);
    }

    private void resumeTracking() {
        sendServiceAction(TrackingService.ACTION_RESUME);
        clActionRow.setVisibility(View.GONE);
        btnJeda.setVisibility(View.VISIBLE);
        llPauseButtons.setVisibility(View.GONE);
        llDashStatusBar.setVisibility(View.GONE);
    }

    private void finishTracking() {
        if (trackingService == null)
            return;

        int secs = trackingService.getSeconds();
        double dist = trackingService.getCurrentDistance();
        ArrayList<GeoPoint> pts = trackingService.getRoutePoints();

        sendServiceAction(TrackingService.ACTION_STOP);

        // Format durasi ke bentuk yang ramah untuk disimpan ke form
        int totalMin = secs / 60;
        String formattedDurationForForm;
        if (totalMin <= 0) {
            formattedDurationForForm = secs + "d";
        } else if (totalMin < 60) {
            formattedDurationForForm = totalMin + " Menit";
        } else {
            int hours = totalMin / 60;
            int remainingMinutes = totalMin % 60;
            formattedDurationForForm = remainingMinutes == 0 ? hours + " Jam"
                    : hours + " Jam " + remainingMinutes + " Menit";
        }

        String formattedDistance = String.format(Locale.getDefault(), "%.2f", dist);
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < pts.size(); i++) {
            GeoPoint pt = pts.get(i);
            pathBuilder.append(pt.getLatitude()).append(",").append(pt.getLongitude());
            if (i < pts.size() - 1)
                pathBuilder.append(";");
        }

        Intent intent = new Intent(requireContext(), FormActivity.class);
        intent.putExtra("EXTRA_DURATION", formattedDurationForForm);
        intent.putExtra("EXTRA_DISTANCE", formattedDistance);
        intent.putExtra("EXTRA_PATH", pathBuilder.toString());
        startActivity(intent);

        resetState();
    }

    private void syncUIWithService() {
        if (trackingService != null && trackingService.getSeconds() > 0) {
            if (trackingService.isTracking()) {
                clActionRow.setVisibility(View.GONE);
                btnJeda.setVisibility(View.VISIBLE);
                llPauseButtons.setVisibility(View.GONE);
                llDashStatusBar.setVisibility(View.GONE);
            } else {
                clActionRow.setVisibility(View.GONE);
                btnJeda.setVisibility(View.GONE);
                llPauseButtons.setVisibility(View.VISIBLE);
                llDashStatusBar.setVisibility(View.VISIBLE);
            }
            cvStopwatch.setVisibility(View.VISIBLE);
            livePolyline.setPoints(trackingService.getRoutePoints());
            mapView.invalidate();
            updateDashboardStats(trackingService.getSeconds(), trackingService.getCurrentDistance());
        }
    }

    private void updateDashboardStats(int secs, double dist) {
        String timeFormatted = formatTime(secs);
        tvStopwatch.setText(timeFormatted);
        tvDashStopwatch.setText(timeFormatted);
        tvDashDistanceVal.setText(String.format(Locale.getDefault(), "%.2f", dist));

        if (secs > 2 && dist > 0.01) {
            double paceInSeconds = (double) secs / dist;
            int paceMin = (int) (paceInSeconds / 60);
            int paceSec = (int) (paceInSeconds % 60);
            tvDashSplitVal.setText(String.format(Locale.getDefault(), "%02d:%02d", paceMin, paceSec));
        } else {
            tvDashSplitVal.setText("---");
        }
    }

    private void resetState() {
        livePolyline.setPoints(new ArrayList<>());
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

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
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

    private void showSportPickerDialog() {
        String[] sportNames = { "Berlari", "Bersepeda", "Jalan Kaki", "Trail Run" };
        int[] sportIcons = { R.drawable.ic_shoe, R.drawable.ic_bike, R.drawable.ic_walk, R.drawable.ic_hiking };
        new AlertDialog.Builder(requireContext())
                .setTitle("Pilih Jenis Olahraga")
                .setItems(sportNames, (dialog, which) -> {
                    selectedSport = sportNames[which];
                    selectedSportIconRes = sportIcons[which];
                    if (tvSportName != null)
                        tvSportName.setText(selectedSport);
                    if (ivSportIcon != null)
                        ivSportIcon.setImageResource(selectedSportIconRes);
                }).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(requireContext(), TrackingService.class);
        requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
            requireActivity().unbindService(connection);
            isBound = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null)
            mapView.onResume();
        if (locationOverlay != null)
            locationOverlay.enableMyLocation();

        IntentFilter filter = new IntentFilter();
        filter.addAction(TrackingService.BROADCAST_TICK);
        filter.addAction(TrackingService.BROADCAST_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(trackingReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(trackingReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null)
            mapView.onPause();
        if (locationOverlay != null)
            locationOverlay.disableMyLocation();
        requireActivity().unregisterReceiver(trackingReceiver);
    }
}