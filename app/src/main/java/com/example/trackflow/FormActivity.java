package com.example.trackflow;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FormActivity extends AppCompatActivity {

    private ActivityHelper activityHelper;
    private int totalMinutes = 0;
    private String finalDurationString = "0 Menit";
    private boolean isFromRecord = false;
    private TextView tvDurationDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        EditText edtTitle = findViewById(R.id.edtTitle);
        EditText edtDistance = findViewById(R.id.edtDistance);
        EditText edtDuration = findViewById(R.id.edtDuration);
        EditText edtDate = findViewById(R.id.edtDate);
        Button btnSave = findViewById(R.id.btnSave);

        // Komponen Tambah Menit Instan
        LinearLayout layoutQuickDuration = findViewById(R.id.layoutQuickDuration);
        tvDurationDisplay = findViewById(R.id.tvDurationDisplay);
        Button btnPlus5 = findViewById(R.id.btnPlus5);
        Button btnPlus10 = findViewById(R.id.btnPlus10);
        Button btnPlus15 = findViewById(R.id.btnPlus15);
        Button btnPlus30 = findViewById(R.id.btnPlus30);
        Button btnResetDuration = findViewById(R.id.btnResetDuration);

        // Cek data kiriman dari RecordFragment (Stopwatch)
        String incomingDuration = getIntent().getStringExtra("EXTRA_DURATION");

        if (incomingDuration != null && !incomingDuration.isEmpty()) {
            isFromRecord = true;
            edtDuration.setVisibility(View.VISIBLE);
            edtDuration.setText(incomingDuration);
            edtDuration.setFocusable(false);

            layoutQuickDuration.setVisibility(View.GONE);
            finalDurationString = incomingDuration;
        } else {
            isFromRecord = false;
            edtDuration.setVisibility(View.GONE);
            layoutQuickDuration.setVisibility(View.VISIBLE);

            // Aksi tombol tambah menit kilat
            btnPlus5.setOnClickListener(v -> appendMinutes(5));
            btnPlus10.setOnClickListener(v -> appendMinutes(10));
            btnPlus15.setOnClickListener(v -> appendMinutes(15));
            btnPlus30.setOnClickListener(v -> appendMinutes(30));
            btnResetDuration.setOnClickListener(v -> {
                totalMinutes = 0;
                appendMinutes(0);
            });

            // --- FITUR BARU: KLIK INDIKATOR UNTUK INPUT PRESISI ---
            tvDurationDisplay.setOnClickListener(v -> showCustomDurationPicker());
        }

        // Setup Date Picker
        edtDate.setFocusable(false);
        edtDate.setClickable(true);
        edtDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(FormActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year1, monthOfYear, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
                        edtDate.setText(sdf.format(cal.getTime()));
                    }, year, month, day);
            // tanggal maksimal yang bisa dipilih adalah HARI INI
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
            datePickerDialog.show();
        });

        activityHelper = ActivityHelper.getInstance(this);
        activityHelper.open();

        btnSave.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String distance = edtDistance.getText().toString().trim();
            String date = edtDate.getText().toString().trim();

            if (title.isEmpty() || distance.isEmpty() || date.isEmpty() || finalDurationString.equals("0 Menit")) {
                Toast.makeText(this, "Semua data harus diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(DatabaseContract.ActivityColumns.COLUMN_TITLE, title);
            values.put(DatabaseContract.ActivityColumns.COLUMN_DISTANCE, distance + " KM");
            values.put(DatabaseContract.ActivityColumns.COLUMN_DURATION, finalDurationString);
            values.put(DatabaseContract.ActivityColumns.COLUMN_DATE, date);

            long result = activityHelper.insert(values);
            if (result > 0) {
                Toast.makeText(this, "Aktivitas berhasil disimpan", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Gagal menyimpan", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Mengakumulasikan tombol penambah menit instan
    private void appendMinutes(int extraMin) {
        totalMinutes += extraMin;
        formatAndDisplayDuration();
    }

    // FITUR POP-UP DIALOG UNTUK MEMILIH MENIT SECARA DETIL & PERSISI
    private void showCustomDurationPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tentukan Menit Latihan");

        // Bikin komponen NumberPicker lewat program Java
        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(300); // Batas maksimal 5 jam latihan (300 menit)
        numberPicker.setValue(totalMinutes > 0 ? totalMinutes : 30); // Default ke 30 menit jika masih 0

        // Masukkan NumberPicker ke dalam dialog box
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(numberPicker);
        linearLayout.setPadding(50, 30, 50, 10);
        builder.setView(linearLayout);

        // Aksi tombol OK pada pop-up
        builder.setPositiveButton("OK", (dialog, which) -> {
            totalMinutes = numberPicker.getValue(); // Ambil angka ganjil/apapun pilihan user
            formatAndDisplayDuration();
        });

        builder.setNegativeButton("BATAL", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Fungsi pusat untuk menyelaraskan ke format Jam/Menit
    private void formatAndDisplayDuration() {
        if (totalMinutes <= 0) {
            totalMinutes = 0;
            finalDurationString = "0 Menit";
        } else if (totalMinutes < 60) {
            finalDurationString = totalMinutes + " Menit";
        } else {
            int hours = totalMinutes / 60;
            int remainingMin = totalMinutes % 60;

            if (remainingMin == 0) {
                finalDurationString = hours + " Jam";
            } else {
                finalDurationString = hours + " Jam " + remainingMin + " Menit";
            }
        }
        tvDurationDisplay.setText(finalDurationString);
    }
}