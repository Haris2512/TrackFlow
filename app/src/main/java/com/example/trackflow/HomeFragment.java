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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private ActivityAdapter adapter;
    private ActivityHelper activityHelper;
    private TextView tvTotalDistance;
    private TextView tvWelcome;
    private SharedPreferences sharedPreferences;
    private android.widget.ImageView ivHomeAvatar;

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
        ivHomeAvatar = view.findViewById(R.id.ivHomeAvatar);
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

        boolean isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false);
        switchTheme.setChecked(isDarkMode);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("DARK_MODE", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Panggil fungsi kalender real-time
        setupRealtimeCalendar(view);
    }

    private void setupRealtimeCalendar(View view) {
        TextView tvDay1 = view.findViewById(R.id.tvDay1);
        TextView tvDate1 = view.findViewById(R.id.tvDate1);
        TextView tvDay2 = view.findViewById(R.id.tvDay2);
        TextView tvDate2 = view.findViewById(R.id.tvDate2);
        TextView tvDayToday = view.findViewById(R.id.tvDayToday);
        TextView tvDateToday = view.findViewById(R.id.tvDateToday);
        TextView tvDay4 = view.findViewById(R.id.tvDay4);
        TextView tvDate4 = view.findViewById(R.id.tvDate4);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", new Locale("id", "ID")); // Cth: Sen, Sel
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd", new Locale("id", "ID")); // Cth: 18

        // Mundur 2 hari dari sekarang (H-2)
        cal.add(Calendar.DAY_OF_MONTH, -2);
        tvDay1.setText(dayFmt.format(cal.getTime()));
        tvDate1.setText(dateFmt.format(cal.getTime()));

        // Maju 1 hari (H-1)
        cal.add(Calendar.DAY_OF_MONTH, 1);
        tvDay2.setText(dayFmt.format(cal.getTime()));
        tvDate2.setText(dateFmt.format(cal.getTime()));

        // Maju 1 hari (HARI INI)
        cal.add(Calendar.DAY_OF_MONTH, 1);
        tvDayToday.setText(dayFmt.format(cal.getTime()));
        tvDateToday.setText(dateFmt.format(cal.getTime()));

        // Maju 1 hari (H+1)
        cal.add(Calendar.DAY_OF_MONTH, 1);
        tvDay4.setText(dayFmt.format(cal.getTime()));
        tvDate4.setText(dateFmt.format(cal.getTime()));
    }

    @Override
    public void onResume() {
        super.onResume();
        String savedName = sharedPreferences.getString("USERNAME", "Atlet TrackFlow");
        tvWelcome.setText("Halo, " + savedName);

        String savedAvatarUri = sharedPreferences.getString("USER_AVATAR", null);
        if (savedAvatarUri != null && ivHomeAvatar != null) {
            try {
                ivHomeAvatar.setImageURI(android.net.Uri.parse(savedAvatarUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        loadActivitiesAsync();
    }

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
        tvTotalDistance.setText(String.format(Locale.US, "%.2f KM", total));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activityHelper != null) {
        }
    }
}