package com.example.trackflow;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("users") // Alamat baru di DummyJSON
    Call<UserResponse> getUsers();
}