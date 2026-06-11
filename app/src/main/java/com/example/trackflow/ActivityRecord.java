package com.example.trackflow;

public class ActivityRecord {
    private int id;
    private String title;
    private String distance;
    private String duration;
    private String date;

    public ActivityRecord(int id, String title, String distance, String duration, String date) {
        this.id = id;
        this.title = title;
        this.distance = distance;
        this.duration = duration;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDistance() {
        return distance;
    }

    public String getDuration() {
        return duration;
    }

    public String getDate() {
        return date;
    }
}