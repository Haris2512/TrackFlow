package com.example.trackflow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView ivLogo = findViewById(R.id.ivSplashLogo);
        TextView tvTitle = findViewById(R.id.tvSplashTitle);

        // Sembunyikan dulu, animasi mulai dari kecil ke normal
        ivLogo.setScaleX(0.5f);
        ivLogo.setScaleY(0.5f);
        ivLogo.setAlpha(0f);
        tvTitle.setAlpha(0f);

        // Logo muncul dengan efek pantul
        ivLogo.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(1000)
                .start();

        // Judul menyusul 0.5 detik setelah logo
        tvTitle.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(500)
                .start();

        // Masuk ke halaman utama setelah 2.5 detik
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish(); // Tutup agar tidak bisa kembali ke splash
        }, 2500);
    }
}