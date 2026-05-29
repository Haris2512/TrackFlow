package com.example.trackflow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String PREF_NAME = "TrackFlowPrefs";
    private ImageView ivAvatar;
    private SharedPreferences sharedPref;

    // Peluncur untuk membuka Galeri HP
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // Minta izin baca permanen agar foto tidak hilang saat aplikasi ditutup
                        requireContext().getContentResolver().takePersistableUriPermission(
                                selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // Pasang foto ke tampilan
                        ivAvatar.setImageURI(selectedImageUri);

                        // Simpan URI foto ke memori SharedPreferences
                        sharedPref.edit().putString("USER_AVATAR", selectedImageUri.toString()).apply();
                    }
                }
            }
    );

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
        CardView cvAvatar = view.findViewById(R.id.cvAvatar);

        // Panggil ID ImageView yang baru ditambahkan di XML
        ivAvatar = view.findViewById(R.id.ivAvatar);

        Context context = getContext();
        if (context == null) return;

        sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 1. Load Nama
        if (tvSavedName != null) {
            String savedName = sharedPref.getString("USERNAME", "Belum ada nama");
            tvSavedName.setText(savedName);
        }

        // 2. Load Tanggal Join
        if (tvJoinDate != null) {
            String joinDate = sharedPref.getString("JOIN_DATE", null);
            if (joinDate == null) {
                String currentDate = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(new Date());
                joinDate = "Bergabung sejak " + currentDate;
                sharedPref.edit().putString("JOIN_DATE", joinDate).apply();
            }
            tvJoinDate.setText(joinDate);
        }

        // 3. Load Foto Profil (Jika sebelumnya sudah pernah ganti foto)
        String savedAvatarUri = sharedPref.getString("USER_AVATAR", null);
        if (savedAvatarUri != null && ivAvatar != null) {
            try {
                ivAvatar.setImageURI(Uri.parse(savedAvatarUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 4. Aksi klik pada bingkai foto profil untuk buka Galeri
        if (cvAvatar != null) {
            cvAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            });
        }

        // 5. Tombol Simpan Nama
        if (btnSaveProfile != null && etName != null && tvSavedName != null) {
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
}