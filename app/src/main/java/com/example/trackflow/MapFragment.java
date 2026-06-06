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

        // Sumber peta standar yang sangat detail
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        IMapController mapController = mapView.getController();
        mapController.setZoom(19.0); // Set awal zoom yang dekat ke bangunan
        GeoPoint defaultLocation = new GeoPoint(-5.147665, 119.432731); // Default Makassar
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
                mapController.setZoom(20.0); // Diperdekat ke level maksimal bangunan (Gedung Pertanian/Kosan akan kelihatan)

                getAddressFromLocation(myLoc);
            } else {
                Toast.makeText(requireContext(), "Mencari lokasi GPS...", Toast.LENGTH_SHORT).show();
            }
        });

        etSearchMap.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchMap.getText().toString().trim();
                if (!query.isEmpty()) {
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    etSearchMap.clearFocus();

                    Toast.makeText(requireContext(), "Mencari \"" + query + "\"...", Toast.LENGTH_SHORT).show();

                    new Thread(() -> {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(query, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());

                                if (getActivity() != null) {
                                    requireActivity().runOnUiThread(() -> {
                                        if (mapView != null) {
                                            mapView.getController().animateTo(point);
                                            mapView.getController().setZoom(19.0);

                                            String featureName = address.getFeatureName();
                                            String fullAddress = address.getAddressLine(0);
                                            String finalAddress = fullAddress;
                                            if (featureName != null && !fullAddress.startsWith(featureName)) {
                                                finalAddress = featureName + ", " + fullAddress;
                                            }
                                            tvAddress.setText(finalAddress);
                                            Toast.makeText(requireContext(), "Lokasi ditemukan: " + (featureName != null ? featureName : query), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                if (getActivity() != null) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "Lokasi tidak ditemukan", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (getActivity() != null) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "Gagal mencari lokasi (Periksa koneksi internet)", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }).start();
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

        // --- MODIFIKASI TITIK LOKASI MENJADI LINGKARAN BIRU KECIL MAPS ---
        Bitmap blueDot = createSleekBlueDot();
        locationOverlay.setPersonIcon(blueDot);
        locationOverlay.setDirectionArrow(blueDot, blueDot); // Aman dari error argumen OSMDroid
        locationOverlay.setPersonHotspot(blueDot.getWidth() / 2f, blueDot.getHeight() / 2f);

        // Aktifkan lingkaran transparan halus pengukur radius akurasi GPS
        locationOverlay.setDrawAccuracyEnabled(true);
        // -----------------------------------------------------------------

        locationOverlay.enableMyLocation();

        locationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    GeoPoint myLoc = locationOverlay.getMyLocation();
                    if (myLoc != null) {
                        mapView.getController().animateTo(myLoc);
                        mapView.getController().setZoom(20.0); // Kunci zoom maksimal biar detail gedungnya tampak
                        getAddressFromLocation(myLoc);
                    }
                });
            }
        });

        mapView.getOverlays().add(locationOverlay);
    }

    // FUNGSI UNTUK MERANCANG TITIK BIRU SLEEK DAN MINIMALIS (UKURAN DIPERKECIL)
    private Bitmap createSleekBlueDot() {
        int size = 44; // Ukuran diperkecil dari 60 menjadi 44 agar tampak anggun dan presisi
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 1. Lingkaran putih bersih sebagai border luar
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.3f, paint);

        // 2. Stroke bayangan tipis pembatas
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#40000000"));
        paint.setStrokeWidth(1.5f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.3f, paint);

        // 3. Inti lingkaran biru khas Google Maps
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1A73E8")); // Biru Google Maps Material modern
        canvas.drawCircle(size / 2f, size / 2f, size / 3.8f, paint);

        return bitmap;
    }

    private void getAddressFromLocation(GeoPoint point) {
        if (getContext() == null) return;

        tvAddress.setVisibility(View.VISIBLE);
        tvAddress.setText("Sinkronisasi koordinat GPS...");

        new Thread(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(point.getLatitude(), point.getLongitude(), 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    String featureName = address.getFeatureName();
                    String fullAddress = address.getAddressLine(0);

                    String finalAddress = fullAddress;
                    if (featureName != null && !fullAddress.startsWith(featureName)) {
                        finalAddress = featureName + ", " + fullAddress;
                    }

                    final String textToShow = finalAddress;

                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            // Alamat dipindahkan sepenuhnya ke kotak teks atas, PETA BERSIH dari pop-up balon!
                            tvAddress.setText(textToShow);
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> tvAddress.setText("Mencari titik koordinat presisi..."));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> tvAddress.setText("Menampilkan peta lokal (Offline Mode)"));
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