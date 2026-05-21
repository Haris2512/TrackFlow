package com.example.trackflow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private RecyclerView rvActivities;
    private ActivityAdapter adapter;
    private ActivityHelper activityHelper;
    private TextView tvTotalDistance;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvActivities = view.findViewById(R.id.rvActivities);
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        Switch switchTheme = view.findViewById(R.id.switchTheme);

        // Setup RecyclerView
        rvActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ActivityAdapter();
        rvActivities.setAdapter(adapter);

        // Buka koneksi database SQLite
        activityHelper = ActivityHelper.getInstance(requireContext());
        activityHelper.open();

        // Aksi tombol tambah (FAB) untuk input manual
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FormActivity.class);
            startActivity(intent);
        });

        // ===== OHANA MODE (DARK/LIGHT TOGGLE) =====
        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("TrackFlowPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false);
        switchTheme.setChecked(isDarkMode);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("DARK_MODE", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    // Gunakan onResume agar saat kembali dari FormActivity, list otomatis ter-refresh!
    @Override
    public void onResume() {
        super.onResume();
        loadActivitiesAsync();
    }

    // PANEN NILAI BACKGROUND THREAD & SQLITE DI SINI
    private void loadActivitiesAsync() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // PROSES BACKGROUND: Ambil data dari SQLite
            Cursor cursor = activityHelper.queryAll();
            // Asumsikan kamu punya kelas MappingHelper untuk mengubah Cursor jadi ArrayList
            ArrayList<ActivityModel> list = MappingHelper.mapCursorToArrayList(cursor);

            // PROSES MAIN THREAD: Kirim data ke tampilan (UI)
            handler.post(() -> {
                if (list != null) {
                    adapter.setData(list);
                    calculateTotalDistance(list); // Bonus: Menghitung total KM!
                }
            });
        });
    }

    // Fungsi canggih untuk menjumlahkan semua jarak yang ada di database
    private void calculateTotalDistance(ArrayList<ActivityModel> list) {
        double total = 0.0;
        for (ActivityModel item : list) {
            try {
                // Menghilangkan tulisan " KM" untuk mengambil angkanya saja
                String distStr = item.getDistance().toUpperCase().replace(" KM", "").trim();
                total += Double.parseDouble(distStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Update teks Total Petualangan di kotak atas
        tvTotalDistance.setText(String.format("%.2f KM", total));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activityHelper != null) {
            activityHelper.close(); // Selalu tutup DB agar tidak Memory Leak
        }
    }
}