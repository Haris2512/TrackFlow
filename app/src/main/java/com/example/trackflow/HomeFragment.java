package com.example.trackflow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
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
    private TextView tvHomeAvatarLetter;

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
        tvHomeAvatarLetter = view.findViewById(R.id.tvHomeAvatarLetter);
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

        // Setup ViewPager2 untuk kartu Streak (Beruntun Anda)
        ViewPager2 vpStreak = view.findViewById(R.id.vpStreak);
        View dot1 = view.findViewById(R.id.dot1);
        View dot2 = view.findViewById(R.id.dot2);
        View dot3 = view.findViewById(R.id.dot3);

        if (vpStreak != null) {
            vpStreak.setAdapter(new StreakPagerAdapter());
            vpStreak.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    if (dot1 != null) dot1.setBackgroundResource(position == 0 ? R.drawable.bg_circle_white : R.drawable.bg_circle_dark);
                    if (dot2 != null) dot2.setBackgroundResource(position == 1 ? R.drawable.bg_circle_white : R.drawable.bg_circle_dark);
                    if (dot3 != null) dot3.setBackgroundResource(position == 2 ? R.drawable.bg_circle_white : R.drawable.bg_circle_dark);
                }
            });
        }

        // Hubungkan aksi bagikan streak
        LinearLayout btnShareStreak = view.findViewById(R.id.btnShareStreak);
        if (btnShareStreak != null) {
            btnShareStreak.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Streak Lari TrackFlow");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Ayo lihat kemajuan beruntun (streak) lari saya minggu ini di aplikasi TrackFlow! Berlari 3 hari berturut-turut!");
                startActivity(Intent.createChooser(shareIntent, "Bagikan Streak"));
            });
        }

        // Hubungkan fitur pencarian dan notifikasi di Toolbar
        View ivSearchIcon = view.findViewById(R.id.ivSearchIcon);
        if (ivSearchIcon != null) {
            ivSearchIcon.setOnClickListener(v -> showSearchDialog());
        }

        View flNotifications = view.findViewById(R.id.flNotifications);
        if (flNotifications != null) {
            flNotifications.setOnClickListener(v -> showNotificationsDialog());
        }
    }

    private void showNotificationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Notifikasi Aktivitas");
        String[] items = {
            "🏆 Rekor Baru! Anda menyelesaikan lari 5K tercepat.",
            "💬 Komunitas: another rhiez memberikan Kudos pada lari Anda.",
            "⭐ Tips TrackFlow: Lakukan peregangan sebelum berlari."
        };
        builder.setItems(items, null);
        builder.setPositiveButton("TUTUP", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showSearchDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Masukkan kata kunci...");
        input.setTextColor(Color.WHITE);
        input.setPadding(48, 32, 48, 32);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Cari Aktivitas")
            .setView(input)
            .setPositiveButton("CARI", (d, which) -> {
                String query = input.getText().toString().trim().toLowerCase();
                filterActivities(query);
            })
            .setNegativeButton("BATAL", (d, which) -> d.dismiss())
            .setNeutralButton("RESET", (d, which) -> {
                loadActivitiesAsync(); // reload all
            })
            .create();
        dialog.show();
    }

    private void filterActivities(String query) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            Cursor cursor = activityHelper.queryAll();
            ArrayList<ActivityModel> list = MappingHelper.mapCursorToArrayList(cursor);
            ArrayList<ActivityModel> filteredList = new ArrayList<>();
            if (list != null) {
                for (ActivityModel model : list) {
                    if (model.getTitle().toLowerCase().contains(query) || model.getDate().toLowerCase().contains(query)) {
                        filteredList.add(model);
                    }
                }
            }
            handler.post(() -> {
                adapter.setData(filteredList);
                calculateTotalDistance(filteredList);
            });
        });
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
        if (tvDay1 != null) tvDay1.setText(dayFmt.format(cal.getTime()));
        if (tvDate1 != null) tvDate1.setText(dateFmt.format(cal.getTime()));

        // Maju 1 hari (H-1)
        cal.add(Calendar.DAY_OF_MONTH, 1);
        if (tvDay2 != null) tvDay2.setText(dayFmt.format(cal.getTime()));
        if (tvDate2 != null) tvDate2.setText(dateFmt.format(cal.getTime()));

        // Maju 1 hari (HARI INI)
        cal.add(Calendar.DAY_OF_MONTH, 1);
        if (tvDayToday != null) tvDayToday.setText(dayFmt.format(cal.getTime()));
        if (tvDateToday != null) tvDateToday.setText(dateFmt.format(cal.getTime()));

        // Maju 1 hari (H+1)
        cal.add(Calendar.DAY_OF_MONTH, 1);
        if (tvDay4 != null) tvDay4.setText(dayFmt.format(cal.getTime()));
        if (tvDate4 != null) tvDate4.setText(dateFmt.format(cal.getTime()));
    }

    @Override
    public void onResume() {
        super.onResume();
        String savedName = sharedPreferences.getString("USERNAME", "Atlet TrackFlow");
        tvWelcome.setText("Halo, " + savedName);

        String savedAvatarUri = sharedPreferences.getString("USER_AVATAR", null);
        if (savedAvatarUri != null && ivHomeAvatar != null) {
            try {
                ivHomeAvatar.setVisibility(View.VISIBLE);
                if (tvHomeAvatarLetter != null) tvHomeAvatarLetter.setVisibility(View.GONE);
                ivHomeAvatar.setImageURI(android.net.Uri.parse(savedAvatarUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (ivHomeAvatar != null) ivHomeAvatar.setVisibility(View.GONE);
            if (tvHomeAvatarLetter != null) {
                tvHomeAvatarLetter.setVisibility(View.VISIBLE);
                if (!savedName.isEmpty()) {
                    tvHomeAvatarLetter.setText(savedName.substring(0, 1).toLowerCase());
                }
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
    }

    private class StreakPagerAdapter extends RecyclerView.Adapter<StreakPagerAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutId = R.layout.item_streak_slide1;
            if (viewType == 1) layoutId = R.layout.item_streak_slide2;
            else if (viewType == 2) layoutId = R.layout.item_streak_slide3;

            View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (getItemViewType(position) == 0) {
                setupRealtimeCalendar(holder.itemView);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}