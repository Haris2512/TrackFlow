package com.example.trackflow;

import com.google.gson.annotations.SerializedName;

public class User {
    // Karena JSON dari Reqres.in memakai underscore (first_name),
    // kita samakan nama variabelnya
    private String first_name;
    private String last_name;
    private String email;
    private String avatar;

    // --- GETTER ---
    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatar() {
        return avatar;
    }
}