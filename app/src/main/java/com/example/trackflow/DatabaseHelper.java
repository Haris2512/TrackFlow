package com.example.trackflow;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static String DATABASE_NAME = "trackflow_db";
    private static final int DATABASE_VERSION = 2;

    private static final String SQL_CREATE_TABLE_ACTIVITY = String.format(
            "CREATE TABLE %s"
                    + " (%s INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " %s TEXT NOT NULL,"
                    + " %s TEXT NOT NULL,"
                    + " %s TEXT NOT NULL,"
                    + " %s TEXT NOT NULL,"
                    + " %s TEXT)",
            DatabaseContract.ActivityColumns.TABLE_NAME,
            DatabaseContract.ActivityColumns._ID,
            DatabaseContract.ActivityColumns.COLUMN_TITLE,
            DatabaseContract.ActivityColumns.COLUMN_DISTANCE,
            DatabaseContract.ActivityColumns.COLUMN_DURATION,
            DatabaseContract.ActivityColumns.COLUMN_DATE,
            DatabaseContract.ActivityColumns.COLUMN_PATH
    );

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_ACTIVITY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + DatabaseContract.ActivityColumns.TABLE_NAME
                    + " ADD COLUMN " + DatabaseContract.ActivityColumns.COLUMN_PATH + " TEXT");
        } else {
            db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.ActivityColumns.TABLE_NAME);
            onCreate(db);
        }
    }
}