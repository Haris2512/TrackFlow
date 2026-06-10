package com.example.trackflow;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * MapFragment — "Buat Rute" seperti Strava.
 *
 * Alur:
 * 1. GPS otomatis deteksi lokasi user → tampil sebagai titik biru
 * 2. User tap peta (atau cari via Nominatim) untuk menentukan SATU titik tujuan → marker hijau
 * 3. OSRM foot API hitung rute lokasi user → tujuan
 * 4. Polyline oranye tergambar, bottom sheet tampil: jarak, estimasi waktu lari, kesulitan
 * 5. Tombol "Mulai Lari" tersedia
 */
public class MapFragment extends Fragment {

    // ── Views ──
    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private EditText etSearchMap;
    private ImageView ivClearSearch;
    private TextView tvTapHint, tvRouteName, tvDistance, tvDuration, tvDifficulty, tvCurrentLocationMap;
    private ProgressBar progressRoute;
    private FloatingActionButton fabMyLocation, fabClear;
    private MaterialButton btnSetTujuan, btnMulaiLari, btnUbahTujuan;
    private View layoutEmpty, layoutRoute;
    private BottomSheetBehavior<View> sheetBehavior;

    // ── State ──
    private GeoPoint myLocation = null;          // Titik biru (GPS user)
    private GeoPoint destinationPoint = null;    // Titik hijau (tujuan)
    private Marker destinationMarker = null;
    private Polyline routeLine = null;
    private boolean waitingForTap = false;       // Mode menunggu tap peta
    private double routeDistanceKm = 0;

    // ── API ──
    private static final String OSRM_BASE = "https://routing.openstreetmap.de/";
    private NominatimApiService osrmService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx,
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        
        // Konfigurasi OSMDroid agar lancar dan tidak diblokir server OSM:
        // 1. Set User-Agent unik (Wajib bagi OSMDroid agar tidak di-throttle/blokir)
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
        
        // 2. Arahkan direktori penyimpanan cache peta ke internal cache (Solusi Scoped Storage Android 10+)
        java.io.File osmdroidBasePath = new java.io.File(ctx.getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmdroidBasePath);
        java.io.File osmdroidTileCache = new java.io.File(osmdroidBasePath, "tiles");
        Configuration.getInstance().setOsmdroidTileCache(osmdroidTileCache);

        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupMap();
        setupRetrofit();
        setupBottomSheet(view);
        setupListeners();
        checkPermission();
    }

    // ─────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────

    private void bindViews(View v) {
        mapView        = v.findViewById(R.id.mapView);
        etSearchMap    = v.findViewById(R.id.etSearchMap);
        ivClearSearch  = v.findViewById(R.id.ivClearSearch);
        tvTapHint      = v.findViewById(R.id.tvTapHint);
        tvRouteName    = v.findViewById(R.id.tvRouteName);
        tvDistance     = v.findViewById(R.id.tvDistance);
        tvDuration     = v.findViewById(R.id.tvDuration);
        tvDifficulty   = v.findViewById(R.id.tvDifficulty);
        progressRoute  = v.findViewById(R.id.progressRoute);
        fabMyLocation  = v.findViewById(R.id.fabMyLocation);
        fabClear       = v.findViewById(R.id.fabClear);
        btnSetTujuan   = v.findViewById(R.id.btnSetTujuan);
        btnMulaiLari   = v.findViewById(R.id.btnMulaiLari);
        btnUbahTujuan  = v.findViewById(R.id.btnUbahTujuan);
        layoutEmpty    = v.findViewById(R.id.layoutEmpty);
        layoutRoute    = v.findViewById(R.id.layoutRoute);
        tvCurrentLocationMap = v.findViewById(R.id.tvCurrentLocationMap);
    }

    private void setupMap() {
        mapView.setMultiTouchControls(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(-5.147665, 119.432731));

        // Tap listener
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (waitingForTap) {
                    setDestination(p);
                    return true;
                }
                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                // Long press juga bisa set tujuan
                setDestination(p);
                return true;
            }
        });
        mapView.getOverlays().add(0, eventsOverlay);
    }

    private void setupRetrofit() {
        osrmService = new Retrofit.Builder()
                .baseUrl(OSRM_BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NominatimApiService.class);
    }

    private void setupBottomSheet(View view) {
        View sheet = view.findViewById(R.id.bottomSheet);
        sheetBehavior = BottomSheetBehavior.from(sheet);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        sheetBehavior.setPeekHeight(dpToPx(180));
    }

    private void setupListeners() {
        // Tombol "Tentukan Tujuan di Peta"
        btnSetTujuan.setOnClickListener(v -> activateTapMode());

        // FAB kembali ke lokasi
        fabMyLocation.setOnClickListener(v -> goToMyLocation());

        // FAB hapus tujuan
        fabClear.setOnClickListener(v -> clearDestination());

        // Ubah tujuan
        btnUbahTujuan.setOnClickListener(v -> clearDestination());

        // Mulai lari -> Tampilkan fake premium upsell dialog
        btnMulaiLari.setOnClickListener(v -> showPremiumDialog(v));

        // Search bar — Nominatim / Geocoder
        etSearchMap.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        etSearchMap.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                searchAndSetDestination(etSearchMap.getText().toString().trim());
                return true;
            }
            return false;
        });

        ivClearSearch.setOnClickListener(v -> {
            etSearchMap.setText("");
            ivClearSearch.setVisibility(View.GONE);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // TAP MODE & DESTINATION
    // ─────────────────────────────────────────────────────────────────────

    /** Aktifkan mode tap — user akan pilih satu titik di peta */
    private void activateTapMode() {
        if (myLocation == null) {
            Toast.makeText(requireContext(),
                    "Menunggu GPS... Pastikan GPS aktif", Toast.LENGTH_SHORT).show();
            return;
        }
        waitingForTap = true;
        tvTapHint.setVisibility(View.VISIBLE);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        Toast.makeText(requireContext(), "Ketuk peta untuk menentukan tujuan larimu", Toast.LENGTH_SHORT).show();
    }

    /**
     * Dipanggil saat user mengetuk peta.
     * Pasang marker hijau di titik tujuan, lalu panggil OSRM.
     */
    private void setDestination(GeoPoint destination) {
        waitingForTap = false;
        tvTapHint.setVisibility(View.GONE);
        destinationPoint = destination;

        // Hapus marker lama
        if (destinationMarker != null) mapView.getOverlays().remove(destinationMarker);

        // Marker hijau untuk tujuan
        destinationMarker = new Marker(mapView);
        destinationMarker.setPosition(destination);
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        destinationMarker.setIcon(createGreenMarkerIcon());
        destinationMarker.setTitle("Tujuan");
        mapView.getOverlays().add(destinationMarker);
        mapView.invalidate();

        // Tampil FAB clear
        fabClear.setVisibility(View.VISIBLE);

        // Ambil nama lokasi tujuan via Geocoder (background)
        getLocationName(destination);

        // Hitung rute via OSRM
        if (myLocation != null) {
            showLoading(true);
            fetchRoute(myLocation, destination);
        }
    }

    /** Hapus tujuan dan kembali ke state awal */
    private void clearDestination() {
        waitingForTap = false;
        destinationPoint = null;

        if (destinationMarker != null) {
            mapView.getOverlays().remove(destinationMarker);
            destinationMarker = null;
        }
        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
            routeLine = null;
        }
        mapView.invalidate();

        fabClear.setVisibility(View.GONE);
        tvTapHint.setVisibility(View.GONE);
        showEmptyState();
    }

    // ─────────────────────────────────────────────────────────────────────
    // OSRM API — Hitung Rute (pejalan kaki/lari)
    // ─────────────────────────────────────────────────────────────────────

    private void fetchRoute(GeoPoint from, GeoPoint to) {
        // OSRM "routed-foot" = mode pejalan kaki/lari, bukan mobil
        String url = String.format(Locale.US,
                "https://routing.openstreetmap.de/routed-foot/route/v1/foot/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                from.getLongitude(), from.getLatitude(),
                to.getLongitude(), to.getLatitude());

        osrmService.getRoute(url).enqueue(new Callback<OsrmResponse>() {
            @Override
            public void onResponse(@NonNull Call<OsrmResponse> call,
                                   @NonNull Response<OsrmResponse> response) {
                showLoading(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null
                        && response.body().getBestRoute() != null) {
                    OsrmResponse.Route route = response.body().getBestRoute();
                    drawRoute(route);
                    updateRouteStats(route);
                } else {
                    // Fallback: garis lurus + estimasi haversine
                    drawStraightLine(from, to);
                    updateStatsFromHaversine(from, to);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OsrmResponse> call, @NonNull Throwable t) {
                showLoading(false);
                if (!isAdded()) return;
                // Fallback tetap tampilkan estimasi garis lurus
                drawStraightLine(from, to);
                updateStatsFromHaversine(from, to);
                Toast.makeText(requireContext(),
                        "Koneksi lambat — menampilkan estimasi langsung", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoute(OsrmResponse.Route route) {
        if (routeLine != null) mapView.getOverlays().remove(routeLine);

        if (route.getGeometry() != null && route.getGeometry().getCoordinates() != null) {
            List<GeoPoint> pts = new ArrayList<>();
            for (List<Double> c : route.getGeometry().getCoordinates()) {
                if (c.size() >= 2) pts.add(new GeoPoint(c.get(1), c.get(0)));
            }
            routeLine = makePolyline(pts);
            mapView.getOverlays().add(routeLine);
            mapView.invalidate();

            // Zoom agar seluruh rute kelihatan
            if (pts.size() > 1) {
                GeoPoint mid = pts.get(pts.size() / 2);
                mapView.getController().animateTo(mid);
            }
        }
    }

    private void drawStraightLine(GeoPoint from, GeoPoint to) {
        if (routeLine != null) mapView.getOverlays().remove(routeLine);
        List<GeoPoint> pts = new ArrayList<>();
        pts.add(from); pts.add(to);
        routeLine = makePolyline(pts);
        mapView.getOverlays().add(routeLine);
        mapView.invalidate();
    }

    private Polyline makePolyline(List<GeoPoint> pts) {
        Polyline line = new Polyline();
        line.setPoints(pts);
        line.getOutlinePaint().setColor(Color.parseColor("#FC4C02"));
        line.getOutlinePaint().setStrokeWidth(14f);
        line.getOutlinePaint().setAlpha(220);
        return line;
    }

    // ─────────────────────────────────────────────────────────────────────
    // STATS & UI
    // ─────────────────────────────────────────────────────────────────────

    private void updateRouteStats(OsrmResponse.Route route) {
        routeDistanceKm = route.getDistance() / 1000.0;
        // Estimasi waktu lari: pace 5:30/km = 330 detik/km
        double runSecs = routeDistanceKm * 330.0;
        String timeStr = formatTime(runSecs);
        String distStr = String.format(Locale.US, "%.1f", routeDistanceKm);
        String difficulty = routeDistanceKm < 3 ? "Mudah"
                : routeDistanceKm < 7 ? "Sedang" : "Sulit";
        int diffColor = routeDistanceKm < 3 ? Color.parseColor("#4CAF50")
                : routeDistanceKm < 7 ? Color.parseColor("#FF9800")
                : Color.parseColor("#F44336");

        requireActivity().runOnUiThread(() -> {
            tvDistance.setText(distStr);
            tvDuration.setText(timeStr);
            tvDifficulty.setText(difficulty);
            tvDifficulty.setTextColor(diffColor);
            showRouteState();
        });
    }

    private void updateStatsFromHaversine(GeoPoint from, GeoPoint to) {
        double meters = haversine(from, to);
        routeDistanceKm = meters / 1000.0;
        double runSecs  = routeDistanceKm * 330.0;
        String distStr  = String.format(Locale.US, "%.1f", routeDistanceKm);
        String timeStr  = formatTime(runSecs);
        String difficulty = routeDistanceKm < 3 ? "Mudah"
                : routeDistanceKm < 7 ? "Sedang" : "Sulit";

        requireActivity().runOnUiThread(() -> {
            tvDistance.setText(distStr);
            tvDuration.setText(timeStr);
            tvDifficulty.setText(difficulty);
            showRouteState();
        });
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutRoute.setVisibility(View.GONE);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void showRouteState() {
        layoutEmpty.setVisibility(View.GONE);
        layoutRoute.setVisibility(View.VISIBLE);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showLoading(boolean show) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            progressRoute.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                layoutEmpty.setVisibility(View.GONE);
                layoutRoute.setVisibility(View.VISIBLE);
                tvDistance.setText("...");
                tvDuration.setText("...");
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    /** Reverse geocode titik tujuan untuk tampilkan nama di route card */
    private void getLocationName(GeoPoint point) {
        new Thread(() -> {
            try {
                Geocoder gc = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> list = gc.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
                if (list != null && !list.isEmpty()) {
                    Address a = list.get(0);
                    String name = a.getFeatureName() != null ? a.getFeatureName()
                            : a.getAddressLine(0).split(",")[0];
                    requireActivity().runOnUiThread(() -> tvRouteName.setText(name));
                }
            } catch (IOException e) { /* abaikan */ }
        }).start();
    }

    /** Cari lokasi via Geocoder dan jadikan tujuan secara otomatis */
    private void searchAndSetDestination(String query) {
        if (query.isEmpty()) return;
        new Thread(() -> {
            try {
                Geocoder gc = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> results = gc.getFromLocationName(query, 1);
                if (results != null && !results.isEmpty()) {
                    Address a = results.get(0);
                    GeoPoint pt = new GeoPoint(a.getLatitude(), a.getLongitude());
                    requireActivity().runOnUiThread(() -> {
                        mapView.getController().animateTo(pt);
                        mapView.getController().setZoom(16.0);
                        setDestination(pt);
                        tvRouteName.setText(a.getFeatureName() != null
                                ? a.getFeatureName() : query);
                    });
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Lokasi tidak ditemukan", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Gagal mencari lokasi", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // GPS
    // ─────────────────────────────────────────────────────────────────────

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        locationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()), mapView);

        Bitmap dot = makeBlueDot();
        locationOverlay.setPersonIcon(dot);
        locationOverlay.setDirectionArrow(dot, dot);
        locationOverlay.setPersonHotspot(dot.getWidth() / 2f, dot.getHeight() / 2f);
        locationOverlay.setDrawAccuracyEnabled(true);
        locationOverlay.enableMyLocation();

        locationOverlay.runOnFirstFix(() -> {
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                GeoPoint loc = locationOverlay.getMyLocation();
                if (loc != null) {
                    myLocation = loc;
                    mapView.getController().animateTo(loc);
                    mapView.getController().setZoom(16.0);
                    updateCurrentLocationText(loc);
                }
            });
        });

        mapView.getOverlays().add(locationOverlay);
    }

    private void goToMyLocation() {
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            myLocation = locationOverlay.getMyLocation();
            mapView.getController().animateTo(myLocation);
            mapView.getController().setZoom(17.0);
            updateCurrentLocationText(myLocation);
        } else {
            Toast.makeText(requireContext(), "Menunggu sinyal GPS...", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCurrentLocationText(GeoPoint point) {
        if (tvCurrentLocationMap == null) return;
        new Thread(() -> {
            try {
                Geocoder gc = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> list = gc.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
                if (list != null && !list.isEmpty()) {
                    Address a = list.get(0);
                    String city = a.getSubAdminArea();
                    if (city == null) city = a.getLocality();
                    String fullText = "Lokasi Anda: " + (city != null ? city : "Terlacak");
                    requireActivity().runOnUiThread(() -> tvCurrentLocationMap.setText(fullText));
                }
            } catch (Exception e) { /* abaikan */ }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /** Lingkaran biru khas Google/Strava untuk titik user */
    private Bitmap makeBlueDot() {
        int size = 48;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Lingkaran putih luar
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, p);

        // Shadow tipis
        p.setStyle(Paint.Style.STROKE);
        p.setColor(0x30000000);
        p.setStrokeWidth(2f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, p);

        // Inti biru
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#1A73E8"));
        canvas.drawCircle(size / 2f, size / 2f, size / 3.6f, p);

        return bmp;
    }

    /** Marker hijau bulat untuk titik tujuan */
    private android.graphics.drawable.Drawable createGreenMarkerIcon() {
        int size = 56;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Lingkaran putih luar
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, p);

        // Stroke shadow
        p.setStyle(Paint.Style.STROKE);
        p.setColor(0x30000000);
        p.setStrokeWidth(2f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, p);

        // Inti hijau
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#2E7D32")); // Hijau gelap
        canvas.drawCircle(size / 2f, size / 2f, size / 3.2f, p);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bmp);
    }

    /** Format detik ke format "j:mm" atau "mm:ss" */
    private String formatTime(double totalSeconds) {
        long mins = (long) totalSeconds / 60;
        long secs = (long) totalSeconds % 60;
        if (mins >= 60) {
            long hrs = mins / 60;
            long m   = mins % 60;
            return String.format(Locale.US, "%dj %02dm", hrs, m);
        }
        return String.format(Locale.US, "%d:%02d", mins, secs);
    }

    /** Hitung jarak lurus (meter) antara dua titik GPS via Haversine */
    private double haversine(GeoPoint a, GeoPoint b) {
        double R = 6371000;
        double dLat = Math.toRadians(b.getLatitude()  - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double x = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(a.getLatitude()))
                 * Math.cos(Math.toRadians(b.getLatitude()))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    }

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void showPremiumDialog(View viewForNavigation) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_premium);
        
        dialog.findViewById(R.id.ivClosePremium).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnLanjutkanPremium).setOnClickListener(v -> {
            dialog.dismiss();
            
            // Ide: Tampilkan peringatan bahwa metode pembayaran tidak valid / belum berlangganan
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Gagal Mengaktifkan Trial")
                    .setMessage("Metode pembayaran tidak valid atau belum ditambahkan. Anda harus berlangganan versi Premium sungguhan untuk merekam rute kustom ini! \n\n(Simulasi Fitur Premium)")
                    .setPositiveButton("Kembali", null)
                    .show();
        });
        
        dialog.show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(
                requireView().getWindowToken(), 0);
        etSearchMap.clearFocus();
    }

    @Override public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    @Override public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }
}