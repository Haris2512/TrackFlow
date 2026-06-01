package com.example.trackflow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Locale;

public class RecordFragment extends Fragment {

    private TextView tvStopwatch;
    private FloatingActionButton fabPlay;
    private LinearLayout llActionButtons;
    private Button btnSelesai;
    private Button btnBatal;

    private boolean isRunning = false;
    private int seconds = 0;
    private Handler handler = new Handler(Looper.getMainLooper());

    public RecordFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStopwatch = view.findViewById(R.id.tvStopwatch);
        fabPlay = view.findViewById(R.id.fabPlay);
        llActionButtons = view.findViewById(R.id.llActionButtons);
        btnSelesai = view.findViewById(R.id.btnSelesai);
        btnBatal = view.findViewById(R.id.btnBatal);

        // Aksi ketika tombol oranye ditekan
        fabPlay.setOnClickListener(v -> {
            if (isRunning) {
                // PAUSE
                isRunning = false;
                fabPlay.setImageResource(android.R.drawable.ic_media_play); // Ubah jadi icon Play
                llActionButtons.setVisibility(View.VISIBLE); // Munculkan tombol BATAL & SELESAI
            } else {
                // START / RESUME
                isRunning = true;
                fabPlay.setImageResource(android.R.drawable.ic_media_pause); // Ubah jadi icon Pause
                llActionButtons.setVisibility(View.GONE); // Sembunyikan tombol aksi
                runStopwatch();
            }
        });

        // Aksi ketika tombol BATAL ditekan
        btnBatal.setOnClickListener(v -> {
            isRunning = false;
            seconds = 0; // Reset waktu
            tvStopwatch.setText("00:00:00"); // Kembalikan teks stopwatch

            // Sembunyikan tombol & kembalikan icon ke posisi awal
            llActionButtons.setVisibility(View.GONE);
            fabPlay.setImageResource(android.R.drawable.ic_media_play);
        });

        // Aksi ketika tombol SELESAI ditekan
        btnSelesai.setOnClickListener(v -> {
            isRunning = false;

            // --- REVISI LOGIKA KONVERSI STRUKTUR WAKTU TEKS ---
            int totalMin = seconds / 60; // Konversi total detik berjalan ke menit
            String formattedDurationForForm = "0 Menit";

            if (totalMin <= 0) {
                // Jika user baru mencoba beberapa detik lalu klik selesai, bulatkan ke 1 Menit agar data valid
                formattedDurationForForm = "1 Menit";
            } else if (totalMin < 60) {
                // Jika di bawah 60 menit, simpan dalam format Menit saja
                formattedDurationForForm = totalMin + " Menit";
            } else {
                // Jika menyentuh 60 menit atau lebih, pecah menjadi satuan Jam dan sisa Menit
                int hours = totalMin / 60;
                int remainingMinutes = totalMin % 60;

                if (remainingMinutes == 0) {
                    formattedDurationForForm = hours + " Jam";
                } else {
                    formattedDurationForForm = hours + " Jam " + remainingMinutes + " Menit";
                }
            }
            // --------------------------------------------------

            // Pindah ke FormActivity dan bawa durasi pintar yang sudah diselaraskan
            Intent intent = new Intent(requireContext(), FormActivity.class);
            intent.putExtra("EXTRA_DURATION", formattedDurationForForm);
            startActivity(intent);

            // Reset komponen setelah data dilempar
            seconds = 0;
            tvStopwatch.setText("00:00:00");
            llActionButtons.setVisibility(View.GONE);
            fabPlay.setImageResource(android.R.drawable.ic_media_play);
        });
    }

    // Logika perhitungan waktu di belakang layar
    private void runStopwatch() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    tvStopwatch.setText(formatTime(seconds));
                    seconds++;
                    handler.postDelayed(this, 1000); // Ulangi setiap 1 detik
                }
            }
        });
    }

    // Mengubah hitungan detik menjadi format jam:menit:detik untuk visual layar berjalan
    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false; // Pastikan waktu berhenti kalau halaman ditutup
    }
}