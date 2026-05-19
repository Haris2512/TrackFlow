package com.example.trackflow;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    // Mengambil banyak data pengguna (list) dari Reqres.in
    @GET("api/users")
    Call<UserResponse> getUsers(@Query("page") int page);
}