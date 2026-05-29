package com.example.trackflow;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class FormActivity extends AppCompatActivity {

    private ActivityHelper activityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        EditText edtTitle = findViewById(R.id.edtTitle);
        EditText edtDistance = findViewById(R.id.edtDistance);
        EditText edtDuration = findViewById(R.id.edtDuration);
        EditText edtDate = findViewById(R.id.edtDate);
        Button btnSave = findViewById(R.id.btnSave);

        // Tangkap data durasi yang dikirim dari RecordFragment
        String incomingDuration = getIntent().getStringExtra("EXTRA_DURATION");

        // Jika data ada (artinya pindah halaman lewat tombol Selesai di Stopwatch)
        if (incomingDuration != null) {
            edtDuration.setText(incomingDuration);
        }

        // --- FITUR DATE PICKER (POP-UP KALENDER) ---
        // Matikan fungsi ngetik manual (keyboard tidak akan muncul)
        edtDate.setFocusable(false);
        edtDate.setClickable(true);

        // Saat kotak tanggal ditekan, munculkan kalender
        edtDate.setOnClickListener(v -> {
            // Ambil tanggal hari ini sebagai default kalender
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Buka Pop-up Google Calendar Date Picker
            DatePickerDialog datePickerDialog = new DatePickerDialog(FormActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        // Set teks hasil ke dalam kotak input (Contoh: 18/5/2026)
                        // Note: monthOfYear ditambah 1 karena index bulan di Java mulai dari 0 (Januari = 0)
                        String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                        edtDate.setText(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });
        // -------------------------------------------

        activityHelper = ActivityHelper.getInstance(this);
        activityHelper.open();

        btnSave.setOnClickListener(v -> {
            String title = edtTitle.getText().toString();
            String distance = edtDistance.getText().toString();
            String duration = edtDuration.getText().toString();
            String date = edtDate.getText().toString();

            if (title.isEmpty() || distance.isEmpty() || duration.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Semua data harus diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(DatabaseContract.ActivityColumns.COLUMN_TITLE, title);
            values.put(DatabaseContract.ActivityColumns.COLUMN_DISTANCE, distance + " KM");
            values.put(DatabaseContract.ActivityColumns.COLUMN_DURATION, duration);
            values.put(DatabaseContract.ActivityColumns.COLUMN_DATE, date);

            long result = activityHelper.insert(values);
            if (result > 0) {
                Toast.makeText(this, "Berhasil disimpan", Toast.LENGTH_SHORT).show();
                finish(); // Tutup form dan kembali ke halaman sebelumnya
            } else {
                Toast.makeText(this, "Gagal menyimpan", Toast.LENGTH_SHORT).show();
            }
        });
    }
}