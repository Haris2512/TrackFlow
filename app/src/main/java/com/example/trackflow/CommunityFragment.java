package com.example.trackflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityFragment extends Fragment {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private ImageView ivCommunityAvatar;
    private SharedPreferences sharedPreferences;
    private View llErrorState;
    private com.google.android.material.button.MaterialButton btnRefreshCommunity;

    public CommunityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvUsers = view.findViewById(R.id.rvUsers);
        ivCommunityAvatar = view.findViewById(R.id.ivCommunityAvatar);
        llErrorState = view.findViewById(R.id.llErrorState);
        btnRefreshCommunity = view.findViewById(R.id.btnRefreshCommunity);

        sharedPreferences = requireActivity().getSharedPreferences("TrackFlowPrefs", Context.MODE_PRIVATE);

        btnRefreshCommunity.setOnClickListener(v -> {
            llErrorState.setVisibility(View.GONE);
            rvUsers.setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(), "Mencoba menghubungkan kembali...", Toast.LENGTH_SHORT).show();
            fetchDataFromApi();
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        View cvCommunityAvatar = view.findViewById(R.id.cvCommunityAvatar);
        if (cvCommunityAvatar != null) {
            cvCommunityAvatar.setOnClickListener(v -> {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_profile);
            });
        }

        fetchDataFromApi();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Sinkronkan avatar saat kembali ke halaman ini
        if (sharedPreferences != null && ivCommunityAvatar != null) {
            String savedAvatarUri = sharedPreferences.getString("USER_AVATAR", null);
            if (savedAvatarUri != null) {
                try {
                    ivCommunityAvatar.setImageURI(Uri.parse(savedAvatarUri));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void fetchDataFromApi() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<UserResponse> call = apiService.getUsers();

        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                // isAdded() dipakai supaya tidak crash jika user pindah halaman sebelum data selesai dimuat
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    llErrorState.setVisibility(View.GONE);
                    rvUsers.setVisibility(View.VISIBLE);
                    List<User> userList = response.body().getUsers();
                    adapter = new UserAdapter(userList, sharedPreferences);
                    rvUsers.setAdapter(adapter);
                } else if (isAdded()) {
                    rvUsers.setVisibility(View.GONE);
                    llErrorState.setVisibility(View.VISIBLE);
                    android.widget.TextView tvError = getView().findViewById(R.id.tvErrorMsg);
                    if (tvError != null) tvError.setText("Waduh, server komunitas sedang sibuk. Coba lagi nanti, ya!");
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                if (isAdded()) {
                    rvUsers.setVisibility(View.GONE);
                    llErrorState.setVisibility(View.VISIBLE);
                    android.widget.TextView tvError = getView().findViewById(R.id.tvErrorMsg);
                    if (tvError != null) tvError.setText("Yah, jaringan internet sepertinya terputus.\nSilakan periksa koneksi Anda.");
                    
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Koneksi Terputus")
                        .setMessage("Tidak dapat terhubung ke server komunitas. Pastikan koneksi internet Anda aktif, lalu coba lagi.")
                        .setPositiveButton("Mengerti", null)
                        .show();
                }
            }
        });
    }
}