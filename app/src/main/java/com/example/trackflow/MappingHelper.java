package com.example.trackflow;

import android.database.Cursor;
import java.util.ArrayList;

public class MappingHelper {

    public static ArrayList<ActivityModel> mapCursorToArrayList(Cursor cursor) {
        ArrayList<ActivityModel> activitiesList = new ArrayList<>();

        // Looping untuk membaca data dari database baris per baris
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns._ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns.COLUMN_TITLE));
            String distance = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns.COLUMN_DISTANCE));
            String duration = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns.COLUMN_DURATION));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ActivityColumns.COLUMN_DATE));

            // Masukkan ke dalam "cetakan" ActivityModel yang tadi kita buat
            activitiesList.add(new ActivityModel(id, title, distance, duration, date));
        }

        return activitiesList;
    }
}