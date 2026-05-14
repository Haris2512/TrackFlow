package com.example.trackflow;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("users/1")
    Call<User> getUserProfile();
}