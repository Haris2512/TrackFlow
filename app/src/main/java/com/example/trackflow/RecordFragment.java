package com.example.trackflow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class RecordFragment extends Fragment {

    private TextView tvTimer;
    private Button btnStartPause, btnFinish;
    private boolean isRunning = false;
    private int seconds = 0;
    private Handler handler;
    private Runnable runnable;

    public RecordFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTimer = view.findViewById(R.id.tvTimer);
        btnStartPause = view.findViewById(R.id.btnStartPause);
        btnFinish = view.findViewById(R.id.btnFinish); // SUDAH DIAMANKAN

        // LOGIKA TIMER SUDAH DIKEMBALIKAN
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    seconds++;
                    int minutes = seconds / 60;
                    int secs = seconds % 60;
                    tvTimer.setText(String.format("%02d:%02d", minutes, secs));
                }
                handler.postDelayed(this, 1000);
            }
        };

        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                isRunning = false;
                btnStartPause.setText("LANJUT");
            } else {
                isRunning = true;
                btnStartPause.setText("JEDA");
                handler.post(runnable);
            }
        });

        // HANYA ADA 1 AKSI SELESAI SEKARANG (INTENT)
        btnFinish.setOnClickListener(v -> {
            if (seconds > 0) {
                isRunning = false;
                handler.removeCallbacks(runnable);

                int minutes = seconds / 60;
                int secs = seconds % 60;
                String timeString = String.format("%02d:%02d", minutes, secs);

                Intent intent = new Intent(requireContext(), FormActivity.class);
                intent.putExtra("EXTRA_DURATION", timeString);
                startActivity(intent);

                seconds = 0;
                tvTimer.setText("00:00");
                btnStartPause.setText("MULAI");
            } else {
                Toast.makeText(requireContext(), "Tekan MULAI terlebih dahulu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}