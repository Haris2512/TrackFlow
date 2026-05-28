package com.example.trackflow;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;

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
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);

        mapView.setTileSource(new XYTileSource(
                "CartoDB", 0, 19, 256, ".png",
                new String[]{
                        "https://a.basemaps.cartocdn.com/light_all/",
                        "https://b.basemaps.cartocdn.com/light_all/",
                        "https://c.basemaps.cartocdn.com/light_all/"
                }
        ));

        Button btnRuteLari = view.findViewById(R.id.btnRuteLari);
        Button btnJalurSepeda = view.findViewById(R.id.btnJalurSepeda);
        Button btnTrailRun = view.findViewById(R.id.btnTrailRun);
        FloatingActionButton fabMyLocation = view.findViewById(R.id.fabMyLocation);
        EditText etSearchMap = view.findViewById(R.id.etSearchMap);

        btnRuteLari.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mencari Rute Lari terdekat...", Toast.LENGTH_SHORT).show()
        );

        btnJalurSepeda.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mencari Jalur Sepeda terdekat...", Toast.LENGTH_SHORT).show()
        );

        btnTrailRun.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mencari Trail Run terdekat...", Toast.LENGTH_SHORT).show()
        );

        fabMyLocation.setOnClickListener(v -> {
            if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
                mapView.getController().animateTo(locationOverlay.getMyLocation());
                mapView.getController().setZoom(18.0);
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
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        mapView.getOverlays().add(locationOverlay);
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