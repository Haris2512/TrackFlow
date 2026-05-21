package com.example.trackflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragment extends Fragment {

    private TextInputEditText etName;
    private TextView tvSavedName;
    private SwitchCompat switchTheme;

    // Nama file penyimpanannya
    private static final String PREF_NAME = "TrackFlowPrefs";

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.etName);
        Button btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        tvSavedName = view.findViewById(R.id.tvSavedName);
        switchTheme = view.findViewById(R.id.switchTheme);

        // Buka lemari SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 1. TAMPILKAN DATA YANG SUDAH TERSIMPAN
        String savedName = sharedPreferences.getString("USERNAME", "Belum ada nama");
        boolean isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false); // Default: Terang

        tvSavedName.setText(savedName);
        switchTheme.setChecked(isDarkMode);

        // 2. SIMPAN NAMA BARU
        btnSaveProfile.setOnClickListener(v -> {
            String inputName = etName.getText().toString().trim();
            if (!inputName.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("USERNAME", inputName);
                editor.apply();

                tvSavedName.setText(inputName);
                etName.setText(""); // Kosongkan kolom input
                Toast.makeText(requireContext(), "Nama tersimpan!", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. UBAH TEMA (DARK / LIGHT MODE)
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Simpan status tema ke SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("DARK_MODE", isChecked);
            editor.apply();

            // Terapkan perubahan tema secara Live
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }
}