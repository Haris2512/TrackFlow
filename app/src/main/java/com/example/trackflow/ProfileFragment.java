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
            }
    );

    public ProfileFragment() {}

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

        Context context = getContext();
        if (context == null) return;

        sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Load Nama & Tanggal
        tvSavedName.setText(sharedPref.getString("USERNAME", "Belum ada nama"));
        String joinDate = sharedPref.getString("JOIN_DATE", null);
        if (joinDate == null) {
            joinDate = "Bergabung sejak " + new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(new Date());
            sharedPref.edit().putString("JOIN_DATE", joinDate).apply();
        }
        tvJoinDate.setText(joinDate);

        String savedAvatarUri = sharedPref.getString("USER_AVATAR", null);
        if (savedAvatarUri != null) {
            try { ivAvatar.setImageURI(Uri.parse(savedAvatarUri)); }
            catch (Exception e) { e.printStackTrace(); }
        }

        btnEditProfile.setOnClickListener(v -> showEditOptionsDialog());

        // Buka Database
        activityHelper = ActivityHelper.getInstance(requireContext());
        activityHelper.open();

        loadActivitiesData();

        // LOGIKA KETIKA TANGGAL KALENDER DIKLIK
        calendarViewProfile.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            // Kita langsung kirimkan Tahun, Bulan, dan Tanggal aslinya ke mesin pencari
            checkActivityOnDate(year, month, dayOfMonth);
        });
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

    private void calculateMonthlyProgress(ArrayList<ActivityModel> list) {
        double totalDistance = 0.0;
        int count = list.size();

        for (ActivityModel item : list) {
            try {
                String distStr = item.getDistance().toUpperCase().replace(" KM", "").trim();
                totalDistance += Double.parseDouble(distStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tvMonthlyCount.setText(count + " Kali");
        tvMonthlyDistance.setText(String.format(Locale.US, "%.2f KM", totalDistance));
    }

    // --- MESIN PENCARI TANGGAL SUPER CERDAS (MENCEGAH ERROR FORMAT) ---
    private void checkActivityOnDate(int year, int month, int dayOfMonth) {
        boolean found = false;
        double totalDistanceThatDay = 0.0;
        int activityCount = 0;

        // Siapkan segala kemungkinan variasi tulisan untuk Tanggal
        String targetDay1 = String.valueOf(dayOfMonth); // cth: "5"
        String targetDay2 = String.format(Locale.US, "%02d", dayOfMonth); // cth: "05"

        // Siapkan segala kemungkinan variasi tulisan untuk Bulan
        String targetMonth1 = String.valueOf(month + 1); // cth: "5"
        String targetMonth2 = String.format(Locale.US, "%02d", month + 1); // cth: "05"

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        String targetMonthNameId = new SimpleDateFormat("MMMM", new Locale("id", "ID")).format(cal.getTime()).toLowerCase(); // cth: "mei"
        String targetMonthNameEn = new SimpleDateFormat("MMMM", Locale.ENGLISH).format(cal.getTime()).toLowerCase(); // cth: "may"
        String targetMonthShortId = new SimpleDateFormat("MMM", new Locale("id", "ID")).format(cal.getTime()).toLowerCase(); // cth: "mei"

        // Mulai pencarian di database
        for (ActivityModel item : allActivities) {
            if (item.getDate() != null) {
                String savedDate = item.getDate().toLowerCase().trim();

                // PEMECAH KATA: Pisahkan teks berdasarkan spasi, garis miring(/), atau strip(-)
                String[] parts = savedDate.split("\\W+");

                boolean dayMatch = false;
                boolean monthMatch = false;

                // Cek apakah di dalam potongan teks tersebut ada Tanggal dan Bulan yang cocok
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

                // Jika Tanggal dan Bulan sama-sama terdeteksi, maka itu MATCH! (Tahun tidak diwajibkan)
                if (dayMatch && monthMatch) {
                    found = true;
                    activityCount++;
                    try {
                        String distStr = item.getDistance().toUpperCase().replace(" KM", "").trim();
                        totalDistanceThatDay += Double.parseDouble(distStr);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Tampilkan hasilnya ke layar
        String displayDate = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(cal.getTime());

        if (found) {
            tvDateDetail.setText("🎯 Hebat! Pada " + displayDate + " kamu merekam " + activityCount + " aktivitas dengan total jarak " + String.format(Locale.US, "%.2f KM", totalDistanceThatDay) + ".");
            tvDateDetail.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvDateDetail.setText("💤 Tidak ada aktivitas pada " + displayDate + ". Saatnya berolahraga!");
            tvDateDetail.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void showEditOptionsDialog() {
        String[] options = {"🖼️ Ganti Foto Profil", "✏️ Ganti Nama"};
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
        if (!currentName.equals("Belum ada nama")) input.setText(currentName);

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