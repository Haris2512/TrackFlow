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

    private ActivityAdapter adapter;
    private ActivityHelper activityHelper;
    private TextView tvTotalDistance;
    private TextView tvWelcome;
    private SharedPreferences sharedPreferences;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvActivities = view.findViewById(R.id.rvActivities);
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        Switch switchTheme = view.findViewById(R.id.switchTheme);

        sharedPreferences = requireActivity().getSharedPreferences("TrackFlowPrefs", Context.MODE_PRIVATE);

        rvActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ActivityAdapter();
        rvActivities.setAdapter(adapter);

        activityHelper = ActivityHelper.getInstance(requireContext());
        activityHelper.open();

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FormActivity.class);
            startActivity(intent);
        });

        // Toggle Dark/Light Mode
        boolean isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false);
        switchTheme.setChecked(isDarkMode);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("DARK_MODE", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Sinkronisasi sapaan nama secara real-time dari Profil
        String savedName = sharedPreferences.getString("USERNAME", "Atlet TrackFlow");
        tvWelcome.setText("Halo, " + savedName);

        loadActivitiesAsync();
    }

    // Load data SQLite via Background Thread
    private void loadActivitiesAsync() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Cursor cursor = activityHelper.queryAll();
            ArrayList<ActivityModel> list = MappingHelper.mapCursorToArrayList(cursor);

            handler.post(() -> {
                if (list != null) {
                    adapter.setData(list);
                    calculateTotalDistance(list);
                }
            });
        });
    }

    private void calculateTotalDistance(ArrayList<ActivityModel> list) {
        double total = 0.0;
        for (ActivityModel item : list) {
            try {
                String distStr = item.getDistance().toUpperCase().replace(" KM", "").trim();
                total += Double.parseDouble(distStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tvTotalDistance.setText(String.format(java.util.Locale.US, "%.2f KM", total));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activityHelper != null) {
            activityHelper.close();
        }
    }
}