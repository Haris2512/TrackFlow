package com.example.trackflow;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class DetailAthleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_athlete);

        // Ambil data dari Intent
        String name = getIntent().getStringExtra("EXTRA_NAME");
        String imageUrl = getIntent().getStringExtra("EXTRA_IMAGE");

        if (name == null)
            name = "Atlet TrackFlow";

        // Inisialisasi View
        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView ivAthleteAvatar = findViewById(R.id.ivAthleteAvatar);
        ImageView ivFeedAvatar = findViewById(R.id.ivFeedAvatar);
        TextView tvAthleteName = findViewById(R.id.tvAthleteName);
        TextView tvFeedName = findViewById(R.id.tvFeedName);

        // Pasang data
        tvAthleteName.setText(name);
        tvFeedName.setText(name);

        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).centerCrop().into(ivAthleteAvatar);
            Glide.with(this).load(imageUrl).circleCrop().into(ivFeedAvatar);
        }

        // Fungsikan tombol Back
        btnBack.setOnClickListener(v -> finish());

        // Fungsikan Tombol Dummy Interaktif
        android.view.View.OnClickListener dummyClickListener = v -> {
            android.widget.Toast
                    .makeText(this, "Fitur ini belum tersedia untuk data simulasi.", android.widget.Toast.LENGTH_SHORT)
                    .show();
        };

        findViewById(R.id.btnActionShare).setOnClickListener(dummyClickListener);
        findViewById(R.id.btnActionSummary).setOnClickListener(dummyClickListener);
        findViewById(R.id.btnActionEvent).setOnClickListener(dummyClickListener);
        findViewById(R.id.btnActionActivity).setOnClickListener(dummyClickListener);
        findViewById(R.id.llPost).setOnClickListener(dummyClickListener);
    }
}
