package com.example.trackflow;

import android.content.ContentValues;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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
            // Ubah etDuration menjadi edtDuration agar sesuai dengan deklarasi di atas
            edtDuration.setText(incomingDuration);
        }

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