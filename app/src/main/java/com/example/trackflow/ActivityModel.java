package com.example.trackflow;

public class ActivityModel {
    private int id;
    private String title;
    private String distance;
    private String duration;
    private String date;

    // Constructor kosong (diperlukan)
    public ActivityModel() {
    }

    // Constructor dengan parameter
    public ActivityModel(int id, String title, String distance, String duration, String date) {
        this.id = id;
        this.title = title;
        this.distance = distance;
        this.duration = duration;
        this.date = date;
    }

    // --- GETTER DAN SETTER ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}