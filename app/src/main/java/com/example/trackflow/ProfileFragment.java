package com.example.trackflow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private static final String PREF_NAME = "TrackFlowPrefs";
    private ImageView ivAvatar;
    private TextView tvSavedName;
    private SharedPreferences sharedPref;

    // Komponen Kalender & Progress
    private TextView tvMonthlyDistance;
    private TextView tvMonthlyCount;
    private TextView tvDateDetail;
    private CalendarView calendarViewProfile;
    private TextView tvActiveDaysList;

    private ActivityHelper activityHelper;
    private ArrayList<ActivityModel> allActivities = new ArrayList<>();

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        ivAvatar.setImageURI(selectedImageUri);
                        sharedPref.edit().putString("USER_AVATAR", selectedImageUri.toString()).apply();
                        Toast.makeText(requireContext(), "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSavedName = view.findViewById(R.id.tvSavedName);
        ivAvatar = view.findViewById(R.id.ivAvatar);
        ImageView btnEditProfile = view.findViewById(R.id.btnEditProfile);
        TextView tvJoinDate = view.findViewById(R.id.tvJoinDate);

        tvMonthlyDistance = view.findViewById(R.id.tvMonthlyDistance);
        tvMonthlyCount = view.findViewById(R.id.tvMonthlyCount);
        tvDateDetail = view.findViewById(R.id.tvDateDetail);
        calendarViewProfile = view.findViewById(R.id.calendarViewProfile);
        tvActiveDaysList = view.findViewById(R.id.tvActiveDaysList);

        Context context = getContext();
        if (context == null)
            return;

        sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Load Nama & Tanggal
        tvSavedName.setText(sharedPref.getString("USERNAME", "Belum ada nama"));
        String joinDate = sharedPref.getString("JOIN_DATE", null);
        if (joinDate == null) {
            joinDate = "Bergabung sejak "
                    + new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(new Date());
            sharedPref.edit().putString("JOIN_DATE", joinDate).apply();
        }
        tvJoinDate.setText(joinDate);

        String savedAvatarUri = sharedPref.getString("USER_AVATAR", null);
        if (savedAvatarUri != null) {
            try {
                ivAvatar.setImageURI(Uri.parse(savedAvatarUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnEditProfile.setOnClickListener(v -> showEditOptionsDialog());

        View cvFavoriteActivitiesButton = view.findViewById(R.id.cvFavoriteActivitiesButton);
        if (cvFavoriteActivitiesButton != null) {
            cvFavoriteActivitiesButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), FavoriteActivity.class);
                startActivity(intent);
            });
        }

        // Buka Database
        activityHelper = ActivityHelper.getInstance(requireContext());
        activityHelper.open();

        loadActivitiesData();

        // LOGIKA KETIKA TANGGAL KALENDER DIKLIK
        calendarViewProfile.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            checkActivityOnDate(year, month, dayOfMonth);
        });

        // Sinkronisasi lokasi asli GPS
        fetchLocation();
    }

    private void fetchLocation() {
        android.location.LocationManager lm = (android.location.LocationManager) requireContext()
                .getSystemService(Context.LOCATION_SERVICE);
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            android.location.Location location = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            }
            if (location != null) {
                try {
                    android.location.Geocoder geocoder = new android.location.Geocoder(requireContext(),
                            java.util.Locale.getDefault());
                    java.util.List<android.location.Address> addresses = geocoder
                            .getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String city = addresses.get(0).getSubAdminArea(); // Kabupaten / Kota
                        if (city == null)
                            city = addresses.get(0).getLocality();
                        String country = addresses.get(0).getCountryName();
                        if (city != null && country != null) {
                            TextView tvBio = getView().findViewById(R.id.tvBio);
                            if (tvBio != null)
                                tvBio.setText("📍 " + city + ", " + country);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (activityHelper != null) {
            loadActivitiesData();
        }
    }

    private void loadActivitiesData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Cursor cursor = activityHelper.queryAll();
            ArrayList<ActivityModel> list = MappingHelper.mapCursorToArrayList(cursor);

            handler.post(() -> {
                if (list != null) {
                    allActivities.clear();
                    allActivities.addAll(list);
                    calculateMonthlyProgress(list);
                }
            });
        });
    }

    // --- REVISI LOGIKA FILTER DATA AKTIVITAS BULAN INI SAJA ---
    private void calculateMonthlyProgress(ArrayList<ActivityModel> list) {
        double totalDistanceCurrentMonth = 0.0;
        int countCurrentMonth = 0;

        // Ambil Bulan aktif dan Tahun aktif saat ini dari HP user
        Calendar currentCal = Calendar.getInstance();
        int currentMonthNum = currentCal.get(Calendar.MONTH) + 1; // 1-12
        String currentMonthName = new SimpleDateFormat("MMMM", new Locale("id", "ID")).format(currentCal.getTime())
                .toLowerCase(); // cth: "juni"
        String currentYear = String.valueOf(currentCal.get(Calendar.YEAR)); // cth: "2026"

        java.util.ArrayList<Integer> activeDays = new java.util.ArrayList<>();

        for (ActivityModel item : list) {
            if (item.getDate() != null) {
                String savedDate = item.getDate().toLowerCase().trim();

                // Gunakan pemecah kata untuk memastikan kecocokan bulan & tahun
                String[] parts = savedDate.split("\\W+");
                boolean monthMatch = false;
                boolean yearMatch = false;

                for (String part : parts) {
                    if (part.equals(String.valueOf(currentMonthNum)) ||
                            part.equals(String.format(Locale.US, "%02d", currentMonthNum)) ||
                            part.equals(currentMonthName)) {
                        monthMatch = true;
                    }
                    if (part.equals(currentYear)) {
                        yearMatch = true;
                    }
                }

                // Jika aktivitas terjadi di bulan dan tahun yang sama dengan hari ini, hitung
                // masuk!
                if (monthMatch && yearMatch) {
                    countCurrentMonth++;
                    try {
                        String distStr = item.getDistance().toUpperCase().replace(" KM", "").trim();
                        totalDistanceCurrentMonth += Double.parseDouble(distStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        for (String part : parts) {
                            if (part.matches("\\d+")) {
                                int day = Integer.parseInt(part);
                                if (day >= 1 && day <= 31 && !activeDays.contains(day)) {
                                    activeDays.add(day);
                                }
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Tampilkan hasil filter spesifik bulan berjalan saja
        tvMonthlyCount.setText(countCurrentMonth + " Kali");
        tvMonthlyDistance.setText(String.format(Locale.US, "%.2f KM", totalDistanceCurrentMonth));

        if (tvActiveDaysList != null) {
            if (activeDays.isEmpty()) {
                tvActiveDaysList.setText("📅 Hari aktif bulan ini: Belum ada");
            } else {
                java.util.Collections.sort(activeDays);
                StringBuilder daysStr = new StringBuilder();
                for (int i = 0; i < activeDays.size(); i++) {
                    daysStr.append(activeDays.get(i));
                    if (i < activeDays.size() - 1)
                        daysStr.append(", ");
                }
                tvActiveDaysList.setText("📅 Hari aktif bulan ini: Tanggal " + daysStr.toString());
            }
        }
    }

    // --- MESIN PENCARI TANGGAL SUPER CERDAS (MENCEGAH ERROR FORMAT) ---
    private void checkActivityOnDate(int year, int month, int dayOfMonth) {
        boolean found = false;
        double totalDistanceThatDay = 0.0;
        int activityCount = 0;

        String targetDay1 = String.valueOf(dayOfMonth);
        String targetDay2 = String.format(Locale.US, "%02d", dayOfMonth);

        String targetMonth1 = String.valueOf(month + 1);
        String targetMonth2 = String.format(Locale.US, "%02d", month + 1);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        String targetMonthNameId = new SimpleDateFormat("MMMM", new Locale("id", "ID")).format(cal.getTime())
                .toLowerCase();
        String targetMonthNameEn = new SimpleDateFormat("MMMM", Locale.ENGLISH).format(cal.getTime()).toLowerCase();
        String targetMonthShortId = new SimpleDateFormat("MMM", new Locale("id", "ID")).format(cal.getTime())
                .toLowerCase();

        for (ActivityModel item : allActivities) {
            if (item.getDate() != null) {
                String savedDate = item.getDate().toLowerCase().trim();
                String[] parts = savedDate.split("\\W+");

                boolean dayMatch = false;
                boolean monthMatch = false;

                for (String part : parts) {
                    if (part.equals(targetDay1) || part.equals(targetDay2)) {
                        dayMatch = true;
                    }
                    if (part.equals(targetMonth1) || part.equals(targetMonth2) ||
                            part.equals(targetMonthNameId) || part.equals(targetMonthNameEn) ||
                            part.equals(targetMonthShortId)) {
                        monthMatch = true;
                    }
                }

                if (dayMatch && monthMatch) {
                    found = true;
                    activityCount++;
                    try {
                        String distStr = item.getDistance().toUpperCase().replace(" KM", "").trim();
                        totalDistanceThatDay += Double.parseDouble(distStr);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        String displayDate = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(cal.getTime());

        if (found) {
            tvDateDetail.setText("🎯 Hebat! Pada " + displayDate + " kamu merekam " + activityCount
                    + " aktivitas dengan total jarak " + String.format(Locale.US, "%.2f KM", totalDistanceThatDay)
                    + ".");
            tvDateDetail.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvDateDetail.setText("💤 Tidak ada aktivitas pada " + displayDate + ". Saatnya berolahraga!");
            tvDateDetail.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void showEditOptionsDialog() {
        String[] options = { "🖼️ Ganti Foto Profil", "✏️ Ganti Nama" };
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Pengaturan Profil");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            } else if (which == 1) {
                showEditNameDialog();
            }
        });
        builder.show();
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ubah Nama Panggilan");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        String currentName = sharedPref.getString("USERNAME", "");
        if (!currentName.equals("Belum ada nama"))
            input.setText(currentName);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(50, 0, 50, 0);
        input.setLayoutParams(lp);

        LinearLayout container = new LinearLayout(requireContext());
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("SIMPAN", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                sharedPref.edit().putString("USERNAME", newName).apply();
                tvSavedName.setText(newName);
                Toast.makeText(requireContext(), "Nama berhasil diperbarui!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("BATAL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}