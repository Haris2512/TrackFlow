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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private TextView tvAddress;

    public MapFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.mapView);
        tvAddress = view.findViewById(R.id.tvAddress);
        mapView.setMultiTouchControls(true);

        mapView.setTileSource(TileSourceFactory.MAPNIK);

        IMapController mapController = mapView.getController();
        mapController.setZoom(19.0); // Default Zoom lebih dekat
        GeoPoint defaultLocation = new GeoPoint(-5.147665, 119.432731); // Koordinat Makassar
        mapController.setCenter(defaultLocation);

        Button btnRuteLari = view.findViewById(R.id.btnRuteLari);
        Button btnJalurSepeda = view.findViewById(R.id.btnJalurSepeda);
        Button btnTrailRun = view.findViewById(R.id.btnTrailRun);
        FloatingActionButton fabMyLocation = view.findViewById(R.id.fabMyLocation);
        EditText etSearchMap = view.findViewById(R.id.etSearchMap);

        btnRuteLari.setOnClickListener(v -> Toast.makeText(requireContext(), "Mencari Rute Lari terdekat...", Toast.LENGTH_SHORT).show());
        btnJalurSepeda.setOnClickListener(v -> Toast.makeText(requireContext(), "Mencari Jalur Sepeda terdekat...", Toast.LENGTH_SHORT).show());
        btnTrailRun.setOnClickListener(v -> Toast.makeText(requireContext(), "Mencari Trail Run terdekat...", Toast.LENGTH_SHORT).show());

        fabMyLocation.setOnClickListener(v -> {
            if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
                GeoPoint myLoc = locationOverlay.getMyLocation();
                mapController.animateTo(myLoc);
                mapController.setZoom(19.5); // Zoom masuk lebih dalam

                getAddressFromLocation(myLoc);
            } else {
                Toast.makeText(requireContext(), "Mencari lokasi GPS...", Toast.LENGTH_SHORT).show();
            }
        });

        etSearchMap.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchMap.getText().toString().trim();
                if (!query.isEmpty()) {
                    Toast.makeText(requireContext(), "Mencari lokasi: " + query + "\n(Fitur Geocoding segera hadir)", Toast.LENGTH_LONG).show();
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    etSearchMap.clearFocus();
                }
                return true;
            }
            return false;
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);

        // -------------------------------------------------------------
        // MENGGANTI IKON DEFAULT MENJADI TITIK BIRU GOOGLE MAPS
        // -------------------------------------------------------------
        Bitmap blueDot = createBlueDotBitmap();
        locationOverlay.setPersonIcon(blueDot);
        locationOverlay.setDirectionArrow(blueDot, blueDot);
        // Atur titik pusat rotasi persis di tengah gambar
        locationOverlay.setPersonHotspot(blueDot.getWidth() / 2f, blueDot.getHeight() / 2f);

        // Aktifkan lingkaran transparan pengukur akurasi bawaan
        locationOverlay.setDrawAccuracyEnabled(true);
        // -------------------------------------------------------------

        locationOverlay.enableMyLocation();

        locationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    GeoPoint myLoc = locationOverlay.getMyLocation();
                    if (myLoc != null) {
                        mapView.getController().animateTo(myLoc);
                        mapView.getController().setZoom(19.5);
                        getAddressFromLocation(myLoc);
                    }
                });
            }
        });

        mapView.getOverlays().add(locationOverlay);
    }

    // FUNGSI KHUSUS UNTUK MELUKIS TITIK BIRU (Tanpa butuh file PNG)
    private Bitmap createBlueDotBitmap() {
        int size = 60; // Ukuran titik
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 1. Gambar lingkaran putih (border luar)
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint);

        // 2. Gambar bayangan tepi tipis (biar menyatu dengan peta)
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#33000000")); // Hitam transparan
        paint.setStrokeWidth(2f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint);

        // 3. Gambar lingkaran biru di tengah
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#4285F4")); // Kode Hex Biru Google Maps
        canvas.drawCircle(size / 2f, size / 2f, size / 3.2f, paint);

        return bitmap;
    }

    private void getAddressFromLocation(GeoPoint point) {
        if (getContext() == null) return;

        tvAddress.setVisibility(View.VISIBLE);
        tvAddress.setText("Mendeteksi detail...");

        new Thread(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(point.getLatitude(), point.getLongitude(), 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    // Berusaha menarik data nama gedung (FeatureName) jika tersedia
                    String featureName = address.getFeatureName();
                    String fullAddress = address.getAddressLine(0);

                    // Gabungkan nama gedung + jalan jika nama gedungnya terdeteksi dan tidak sama dengan jalan
                    String finalAddress = fullAddress;
                    if (featureName != null && !fullAddress.startsWith(featureName)) {
                        finalAddress = featureName + ", " + fullAddress;
                    }

                    String textToShow = finalAddress;

                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            tvAddress.setText(textToShow);
                            mapView.invalidate(); // Paksa peta untuk redraw
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> tvAddress.setText("Lokasi detail tidak ditemukan"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> tvAddress.setText("Gagal memuat alamat. Periksa koneksi internet."));
                }
            }
        }).start();
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
}