package com.example.trackflow;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Locale;

public class TrackingService extends Service implements LocationListener {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_STOP = "ACTION_STOP";

    public static final String BROADCAST_TICK = "com.example.trackflow.TRACKING_TICK";
    public static final String BROADCAST_LOCATION = "com.example.trackflow.TRACKING_LOCATION";

    private final IBinder binder = new LocalBinder();
    private LocationManager locationManager;

    private boolean isRunning = false;
    private int seconds = 0;
    private double currentDistance = 0.0;
    private ArrayList<GeoPoint> routePoints = new ArrayList<>();

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    public class LocalBinder extends Binder {
        public TrackingService getService() {
            return TrackingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_START:
                    startTracking();
                    break;
                case ACTION_PAUSE:
                    pauseTracking();
                    break;
                case ACTION_RESUME:
                    resumeTracking();
                    break;
                case ACTION_STOP:
                    stopTracking();
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @SuppressLint("MissingPermission")
    private void startTracking() {
        if (isRunning) return;
        isRunning = true;
        seconds = 0;
        currentDistance = 0.0;
        routePoints.clear();

        startForeground(1, buildNotification("00:00:00", "0.00 km"));

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2f, this);
        }

        runStopwatch();
    }

    private void pauseTracking() {
        isRunning = false;
        locationManager.removeUpdates(this);
        updateNotification("Jeda - " + formatTime(seconds), String.format(Locale.getDefault(), "%.2f km", currentDistance));
    }

    @SuppressLint("MissingPermission")
    private void resumeTracking() {
        isRunning = true;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2f, this);
        }
        runStopwatch();
    }

    private void stopTracking() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        locationManager.removeUpdates(this);
        stopForeground(true);
        stopSelf();
    }

    private void runStopwatch() {
        handler.removeCallbacksAndMessages(null);
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    seconds++;
                    Intent intent = new Intent(BROADCAST_TICK);
                    intent.setPackage(getPackageName());
                    sendBroadcast(intent);

                    updateNotification(formatTime(seconds), String.format(Locale.getDefault(), "%.2f km", currentDistance));
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(timerRunnable);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (!isRunning) return;
        
        GeoPoint gp = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (routePoints.isEmpty()) {
            routePoints.add(gp);
            broadcastLocationUpdate(gp);
        } else {
            GeoPoint lastPt = routePoints.get(routePoints.size() - 1);
            double dist = gp.distanceToAsDouble(lastPt);
            if (dist >= 5.0) {
                routePoints.add(gp);
                currentDistance += (dist / 1000.0);
                broadcastLocationUpdate(gp);
            }
        }
    }

    private void broadcastLocationUpdate(GeoPoint gp) {
        Intent intent = new Intent(BROADCAST_LOCATION);
        intent.setPackage(getPackageName());
        intent.putExtra("lat", gp.getLatitude());
        intent.putExtra("lon", gp.getLongitude());
        sendBroadcast(intent);
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "TRACKING_CHANNEL",
                    "Pelacakan Lari",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Menampilkan status pelacakan olahraga");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String time, String distance) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, "TRACKING_CHANNEL")
                .setContentTitle("Merekam Aktivitas - " + time)
                .setContentText("Jarak: " + distance)
                .setSmallIcon(R.drawable.ic_shoe)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setColor(Color.parseColor("#FC4C02"))
                .build();
    }

    private void updateNotification(String time, String distance) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, buildNotification(time, distance));
        }
    }

    // Getters for bound fragment
    public boolean isTracking() { return isRunning; }
    public int getSeconds() { return seconds; }
    public double getCurrentDistance() { return currentDistance; }
    public ArrayList<GeoPoint> getRoutePoints() { return routePoints; }
}
