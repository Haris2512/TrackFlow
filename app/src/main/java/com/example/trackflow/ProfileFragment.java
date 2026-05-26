package com.example.trackflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private EditText etName;
    private TextView tvSavedName;

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

        // Buka lemari SharedPreferences dengan safely getContext()
        Context context = getContext();
        if (context == null) return;

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 1. TAMPILKAN DATA YANG SUDAH TERSIMPAN (safely check null)
        if (tvSavedName != null && sharedPreferences != null) {
            String savedName = sharedPreferences.getString("USERNAME", "Belum ada nama");
            tvSavedName.setText(savedName);
        }

        // 2. SIMPAN NAMA BARU (safely check null)
        if (btnSaveProfile != null && etName != null && sharedPreferences != null) {
            btnSaveProfile.setOnClickListener(v -> {
                String inputName = etName.getText().toString().trim();
                if (!inputName.isEmpty()) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("USERNAME", inputName);
                    editor.apply();

                    if (tvSavedName != null) {
                        tvSavedName.setText(inputName);
                    }
                    etName.setText(""); // Kosongkan kolom input
                    Toast.makeText(context, "Nama tersimpan!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}