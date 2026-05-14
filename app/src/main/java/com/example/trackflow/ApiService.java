package com.example.trackflow;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    // Mengambil data user dummy dengan ID 2
    @GET("api/users/2")
    Call<UserResponse> getUserProfile();
}