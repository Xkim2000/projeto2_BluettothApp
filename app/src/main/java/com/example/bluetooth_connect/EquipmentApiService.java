package com.example.bluetooth_connect;

import android.service.autofill.UserData;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface EquipmentApiService {
    @GET("equipment")
    Call<List<Device>> getAllEquipment();

    @POST("userdata")
    Call<Integer> insertUserData(@Body List<Record> records);
}
