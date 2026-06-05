package com.example.trackflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView rvFavoriteActivities;
    private LinearLayout llEmptyFavorites;
    private ActivityAdapter adapter;
    private ActivityHelper activityHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        // Bind Views
        CardView cvBackFavorite = findViewById(R.id.cvBackFavorite);
        rvFavoriteActivities = findViewById(R.id.rvFavoriteActivities);
        llEmptyFavorites = findViewById(R.id.llEmptyFavorites);

        // Click Back
        cvBackFavorite.setOnClickListener(v -> finish());

        // Setup RecyclerView
        rvFavoriteActivities.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityAdapter();
        rvFavoriteActivities.setAdapter(adapter);

        // Preferences & DB
        sharedPreferences = getSharedPreferences("TrackFlowPrefs", Context.MODE_PRIVATE);
        activityHelper = ActivityHelper.getInstance(this);
        activityHelper.open();

        loadFavoritesAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoritesAsync();
    }

    private void loadFavoritesAsync() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            Cursor cursor = activityHelper.queryAll();
            ArrayList<ActivityModel> list = MappingHelper.mapCursorToArrayList(cursor);
            ArrayList<ActivityModel> favoritesList = new ArrayList<>();
            if (list != null) {
                for (ActivityModel model : list) {
                    // Check if bookmark flag exists in SharedPreferences
                    boolean isFav = sharedPreferences.getBoolean("FAV_ID_" + model.getId(), false);
                    if (isFav) {
                        favoritesList.add(model);
                    }
                }
            }
            handler.post(() -> {
                if (favoritesList.isEmpty()) {
                    rvFavoriteActivities.setVisibility(View.GONE);
                    llEmptyFavorites.setVisibility(View.VISIBLE);
                } else {
                    rvFavoriteActivities.setVisibility(View.VISIBLE);
                    llEmptyFavorites.setVisibility(View.GONE);
                    adapter.setData(favoritesList);
                }
            });
        });
    }
}
