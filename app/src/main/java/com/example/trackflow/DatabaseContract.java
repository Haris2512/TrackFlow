package com.example.trackflow;

import android.provider.BaseColumns;

public class DatabaseContract {

    // Mencegah class ini diinisialisasi secara tidak sengaja
    private DatabaseContract() {}

    public static final class ActivityColumns implements BaseColumns {
        public static final String TABLE_NAME = "activity_history";
        public static final String COLUMN_TITLE = "title";       // Contoh: "Lari Pagi"
        public static final String COLUMN_DISTANCE = "distance"; // Jarak dalam KM
        public static final String COLUMN_DURATION = "duration"; // Waktu tempuh
        public static final String COLUMN_DATE = "date";         // Tanggal aktivitas
    }
}