package com.example.trackflow;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FormActivity extends AppCompatActivity {

    private ActivityHelper activityHelper;
    private int totalMinutes = 0;
    private String finalDurationString = "0 Menit";
    private boolean isFromRecord = false;
    private TextView tvDurationDisplay;
    private MapView mapViewPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inisialisasi konfigurasi OSMDroid sebelum setContentView
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        
        setContentView(R.layout.activity_form);

        EditText edtTitle = findViewById(R.id.edtTitle);
        EditText edtDistance = findViewById(R.id.edtDistance);
        EditText edtDuration = findViewById(R.id.edtDuration);
        EditText edtDate = findViewById(R.id.edtDate);
        Button btnSave = findViewById(R.id.btnSave);
        ImageView ivBack = findViewById(R.id.ivBack);
        Button btnDiscard = findViewById(R.id.btnDiscard);
        mapViewPreview = findViewById(R.id.mapViewPreview);

        // Komponen Tambah Menit Instan
        CardView cardQuickDuration = findViewById(R.id.cardQuickDuration);
        tvDurationDisplay = findViewById(R.id.tvDurationDisplay);
        Button btnPlus5 = findViewById(R.id.btnPlus5);
        Button btnPlus10 = findViewById(R.id.btnPlus10);
        Button btnPlus15 = findViewById(R.id.btnPlus15);
        Button btnPlus30 = findViewById(R.id.btnPlus30);
        Button btnResetDuration = findViewById(R.id.btnResetDuration);

        // Inisialisasi Map Preview
        if (mapViewPreview != null) {
            mapViewPreview.setTileSource(TileSourceFactory.MAPNIK);
            IMapController controller = mapViewPreview.getController();
            controller.setZoom(17.0);
            controller.setCenter(new GeoPoint(-5.147665, 119.432731)); // Default Makassar
        }

        // Tombol Back & Discard
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }
        if (btnDiscard != null) {
            btnDiscard.setOnClickListener(v -> {
                Toast.makeText(this, "Aktivitas dibuang", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        // Cek data kiriman dari RecordFragment (Stopwatch)
        String incomingDuration = getIntent().getStringExtra("EXTRA_DURATION");
        String incomingDistance = getIntent().getStringExtra("EXTRA_DISTANCE");

        if (incomingDuration != null && !incomingDuration.isEmpty()) {
            isFromRecord = true;
            edtDuration.setVisibility(View.VISIBLE);
            edtDuration.setText(incomingDuration);
            edtDuration.setFocusable(false);

            cardQuickDuration.setVisibility(View.GONE);
            finalDurationString = incomingDuration;
            
            // Auto fill title dan distance untuk mempercepat UI
            edtTitle.setText("Berlari Siang");
            if (incomingDistance != null && !incomingDistance.isEmpty()) {
                edtDistance.setText(incomingDistance);
            } else {
                edtDistance.setText("0.03");
            }
        } else {
            isFromRecord = false;
            edtDuration.setVisibility(View.GONE);
            cardQuickDuration.setVisibility(View.VISIBLE);

            // Aksi tombol tambah menit kilat
            btnPlus5.setOnClickListener(v -> appendMinutes(5));
            btnPlus10.setOnClickListener(v -> appendMinutes(10));
            btnPlus15.setOnClickListener(v -> appendMinutes(15));
            btnPlus30.setOnClickListener(v -> appendMinutes(30));
            btnResetDuration.setOnClickListener(v -> {
                totalMinutes = 0;
                appendMinutes(0);
            });

            // Klik indikator untuk input presisi
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

            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Set date default ke hari ini
        SimpleDateFormat sdfToday = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
        edtDate.setText(sdfToday.format(Calendar.getInstance().getTime()));

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
                
                // Launch DetailActivity matching screenshot 1
                Intent intent = new Intent(FormActivity.this, DetailActivity.class);
                intent.putExtra("EXTRA_TITLE", title);
                intent.putExtra("EXTRA_DISTANCE", distance + " KM");
                intent.putExtra("EXTRA_DURATION", finalDurationString);
                intent.putExtra("EXTRA_DATE", date);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Gagal menyimpan", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void appendMinutes(int extraMin) {
        totalMinutes += extraMin;
        formatAndDisplayDuration();
    }

    private void showCustomDurationPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tentukan Menit Latihan");

        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(300);

        int initialValue = totalMinutes > 0 ? totalMinutes : 30;
        numberPicker.setValue(initialValue);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(numberPicker);
        linearLayout.setPadding(50, 30, 50, 10);
        builder.setView(linearLayout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            totalMinutes = numberPicker.getValue();
            formatAndDisplayDuration();
        });

        builder.setNegativeButton("BATAL", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

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

    @Override
    protected void onResume() {
        super.onResume();
        if (mapViewPreview != null) {
            mapViewPreview.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewPreview != null) {
            mapViewPreview.onPause();
        }
    }
}