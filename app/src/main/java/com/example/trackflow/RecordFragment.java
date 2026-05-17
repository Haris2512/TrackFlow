package com.example.trackflow;

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

    // Variabel untuk logika Stopwatch
    private boolean isRunning = false;
    private int seconds = 0;

    // Ini dia bintang utamanya untuk panen nilai Background Thread!
    private Handler handler;
    private Runnable runnable;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTimer = view.findViewById(R.id.tvTimer);
        btnStartPause = view.findViewById(R.id.btnStartPause);
        btnFinish.setOnClickListener(v -> {
            if (seconds > 0) {
                // 1. Hentikan timer terlebih dahulu
                isRunning = false;
                handler.removeCallbacks(runnable);

                // 2. Format waktu menjadi teks (Menit:Detik)
                int minutes = seconds / 60;
                int secs = seconds % 60;
                String timeString = String.format("%02d:%02d", minutes, secs);

                // 3. Kirim data waktu ke FormActivity menggunakan Intent
                android.content.Intent intent = new android.content.Intent(requireContext(), FormActivity.class);
                intent.putExtra("EXTRA_DURATION", timeString);
                startActivity(intent);

                // 4. Reset Stopwatch kembali ke keadaan awal
                seconds = 0;
                tvTimer.setText("00:00");
                btnStartPause.setText("MULAI");
            } else {
                Toast.makeText(requireContext(), "Tekan MULAI terlebih dahulu!", Toast.LENGTH_SHORT).show();
            }
        });

        // Aksi Tombol Mulai / Jeda
        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                // Jika sedang jalan, maka Jeda (Pause)
                isRunning = false;
                btnStartPause.setText("LANJUT");
            } else {
                // Jika sedang berhenti, maka Mulai (Start)
                isRunning = true;
                btnStartPause.setText("JEDA");
                handler.post(runnable); // Memicu Background Thread berjalan
            }
        });

        // Aksi Tombol Selesai
        btnFinish.setOnClickListener(v -> {
            if (seconds > 0) {
                // Hentikan Background Thread agar tidak jalan terus di belakang layar
                isRunning = false;
                handler.removeCallbacks(runnable);

                int totalMinutes = seconds / 60;
                Toast.makeText(requireContext(), "Misi Selesai! Waktu: " + totalMinutes + " Menit", Toast.LENGTH_LONG).show();

                // (Nanti kita bisa tambahkan logika untuk melempar data ini ke FormActivity)

                // Reset Stopwatch kembali ke 0
                seconds = 0;
                tvTimer.setText("00:00");
                btnStartPause.setText("MULAI");
            } else {
                Toast.makeText(requireContext(), "Tekan MULAI terlebih dahulu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // WAJIB: Hentikan Thread saat fragment ditutup agar aplikasi tidak bocor memori (Memory Leak)
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}