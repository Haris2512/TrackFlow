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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String PREF_NAME = "TrackFlowPrefs";

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etName = view.findViewById(R.id.etName);
        Button btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        TextView tvSavedName = view.findViewById(R.id.tvSavedName);
        TextView tvJoinDate = view.findViewById(R.id.tvJoinDate);

        Context context = getContext();
        if (context == null) return;

        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Load data nama
        String savedName = sharedPref.getString("USERNAME", "Belum ada nama");
        tvSavedName.setText(savedName);

        // Load atau buat tanggal join baru (otomatis)
        String joinDate = sharedPref.getString("JOIN_DATE", null);
        if (joinDate == null) {
            String currentDate = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(new Date());
            joinDate = "Bergabung sejak " + currentDate;
            sharedPref.edit().putString("JOIN_DATE", joinDate).apply();
        }
        tvJoinDate.setText(joinDate);

        // Simpan pembaruan nama
        btnSaveProfile.setOnClickListener(v -> {
            String inputName = etName.getText().toString().trim();
            if (!inputName.isEmpty()) {
                sharedPref.edit().putString("USERNAME", inputName).apply();
                tvSavedName.setText(inputName);
                etName.setText("");
                Toast.makeText(context, "Profil diperbarui!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}