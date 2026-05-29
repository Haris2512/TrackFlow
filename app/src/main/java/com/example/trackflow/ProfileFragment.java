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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String PREF_NAME = "TrackFlowPrefs";
    private ImageView ivAvatar;
    private SharedPreferences sharedPref;

    // Variabel penampung untuk menyimpan foto sementara (sebelum di-save)
    private String tempAvatarUri = null;

    // Peluncur untuk membuka Galeri HP
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // 1. Pasang foto ke tampilan profil sebagai "Preview"
                        ivAvatar.setImageURI(selectedImageUri);

                        // 2. Simpan URI-nya ke memori sementara (Jangan simpan ke database dulu!)
                        tempAvatarUri = selectedImageUri.toString();
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
        ivAvatar = view.findViewById(R.id.ivAvatar);

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

        // Load Foto Profil (Jika sebelumnya sudah pernah simpan)
        String savedAvatarUri = sharedPref.getString("USER_AVATAR", null);
        if (savedAvatarUri != null && ivAvatar != null) {
            try {
                ivAvatar.setImageURI(Uri.parse(savedAvatarUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Klik foto buka galeri
        if (cvAvatar != null) {
            cvAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            });
        }

        // TOMBOL SIMPAN (Simpan Nama, Simpan Foto, lalu pindah halaman)
        if (btnSaveProfile != null && etName != null && tvSavedName != null) {
            btnSaveProfile.setOnClickListener(v -> {
                String inputName = etName.getText().toString().trim();

                if (!inputName.isEmpty()) {
                    SharedPreferences.Editor editor = sharedPref.edit();

                    // 1. Simpan Nama
                    editor.putString("USERNAME", inputName);

                    // 2. Simpan Foto (Hanya jika user memilih foto baru)
                    if (tempAvatarUri != null) {
                        editor.putString("USER_AVATAR", tempAvatarUri);
                    }

                    // Kunci perubahan
                    editor.apply();

                    tvSavedName.setText(inputName);
                    etName.setText("");
                    Toast.makeText(context, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show();

                    // 3. OTOMATIS KEMBALI KE HALAMAN HOME
                    BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
                    if (bottomNav != null) {
                        // Pastikan 'nav_home' ini sama dengan ID menu home kamu di res/menu/bottom_nav_menu.xml
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    } else {
                        // Fallback jika tidak pakai bottom nav
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.nav_host_fragment, new HomeFragment())
                                .commit();
                    }

                } else {
                    Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}