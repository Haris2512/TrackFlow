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

        // 1. Sembunyikan elemen terlebih dahulu (ukuran 50%, transparan)
        ivLogo.setScaleX(0.5f);
        ivLogo.setScaleY(0.5f);
        ivLogo.setAlpha(0f);
        tvTitle.setAlpha(0f);

        // 2. Jalankan Animasi (Membesar dengan efek memantul / Overshoot)
        ivLogo.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(1000)
                .start();

        // Judul muncul perlahan setelah logo setengah jalan membesar
        tvTitle.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(500)
                .start();

        // 3. Pindah ke MainActivity setelah 2.5 detik (Menggunakan Handler)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish(); // Tutup Splash Screen agar tidak bisa di-back
        }, 2500);
    }
}