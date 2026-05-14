package com.example.trackflow;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvActivities;
    private ActivityAdapter adapter;
    private ActivityHelper activityHelper;
    private TextView tvWelcome; //

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi TextView untuk Nama (Networking)
        tvWelcome = view.findViewById(R.id.tvWelcome); //

        rvActivities = view.findViewById(R.id.rvActivities);
        rvActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ActivityAdapter();
        rvActivities.setAdapter(adapter);

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FormActivity.class);
            startActivity(intent);
        });

        activityHelper = ActivityHelper.getInstance(requireContext());
        activityHelper.open();

        loadDataFromSQLite();
        fetchUserProfile(); // (Panggil API)
    }

    // Fungsi untuk mengambil data dari API (Networking)
    private void fetchUserProfile() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<User> call = apiService.getUserProfile();

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Ambil langsung getName()
                    String name = response.body().getName();
                    tvWelcome.setText("Halo, " + name + "!");
                } else {
                    tvWelcome.setText("Error API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                tvWelcome.setText("Gagal: " + t.getMessage());
            }
        });
    }

    private void loadDataFromSQLite() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Cursor cursor = activityHelper.queryAll();
            ArrayList<ActivityRecord> list = new ArrayList<>();

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns._ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns.COLUMN_TITLE));
                    String dist = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns.COLUMN_DISTANCE));
                    String dur = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns.COLUMN_DURATION));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns.COLUMN_DATE));

                    list.add(new ActivityRecord(id, title, dist, dur, date));
                } while (cursor.moveToNext());
                cursor.close();
            }

            handler.post(() -> adapter.setData(list));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataFromSQLite();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activityHelper != null) {
            activityHelper.close();
        }
    }
}