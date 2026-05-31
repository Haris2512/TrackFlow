package com.example.trackflow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String PREF_NAME = "TrackFlowPrefs";
    private ImageView ivAvatar;
    private TextView tvSavedName;
    private SharedPreferences sharedPref;

    // Peluncur untuk membuka Galeri HP
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // Pasang foto dan LANGSUNG SIMPAN
                        ivAvatar.setImageURI(selectedImageUri);
                        sharedPref.edit().putString("USER_AVATAR", selectedImageUri.toString()).apply();
                        Toast.makeText(requireContext(), "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show();
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

        tvSavedName = view.findViewById(R.id.tvSavedName);
        ivAvatar = view.findViewById(R.id.ivAvatar);
        ImageView btnEditProfile = view.findViewById(R.id.btnEditProfile);
        TextView tvJoinDate = view.findViewById(R.id.tvJoinDate);

        Context context = getContext();
        if (context == null) return;

        sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Load Nama
        if (tvSavedName != null) {
            String savedName = sharedPref.getString("USERNAME", "Belum ada nama");
            tvSavedName.setText(savedName);
        }

        // Load Tanggal Join
        if (tvJoinDate != null) {
            String joinDate = sharedPref.getString("JOIN_DATE", null);
            if (joinDate == null) {
                String currentDate = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(new Date());
                joinDate = "Bergabung sejak " + currentDate;
                sharedPref.edit().putString("JOIN_DATE", joinDate).apply();
            }
            tvJoinDate.setText(joinDate);
        }

        // Load Foto Profil
        String savedAvatarUri = sharedPref.getString("USER_AVATAR", null);
        if (savedAvatarUri != null && ivAvatar != null) {
            try {
                ivAvatar.setImageURI(Uri.parse(savedAvatarUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Aksi ketika tombol Edit Profil (Kanan Atas) ditekan
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditOptionsDialog());
        }
    }

    // Menampilkan Pilihan Edit (Nama / Foto)
    private void showEditOptionsDialog() {
        String[] options = {"🖼️ Ganti Foto Profil", "✏️ Ganti Nama"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Pengaturan Profil");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Pilih Ganti Foto (Buka Galeri)
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            } else if (which == 1) {
                // Pilih Ganti Nama (Buka Dialog Input Nama)
                showEditNameDialog();
            }
        });
        builder.show();
    }

    // Menampilkan Pop-up Input untuk Mengganti Nama
    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ubah Nama Panggilan");

        // Membuat kotak input secara dinamis (programmatically)
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Masukkan nama baru...");

        // Ambil nama lama agar user bisa melihatnya
        String currentName = sharedPref.getString("USERNAME", "");
        if (!currentName.equals("Belum ada nama")) {
            input.setText(currentName);
        }

        // Mengatur margin untuk kotak input
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(50, 0, 50, 0);
        input.setLayoutParams(lp);

        LinearLayout container = new LinearLayout(requireContext());
        container.addView(input);
        builder.setView(container);

        // Tombol SIMPAN pada Pop-up
        builder.setPositiveButton("SIMPAN", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                sharedPref.edit().putString("USERNAME", newName).apply();
                tvSavedName.setText(newName);
                Toast.makeText(requireContext(), "Nama berhasil diperbarui!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            }
        });

        // Tombol BATAL pada Pop-up
        builder.setNegativeButton("BATAL", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}