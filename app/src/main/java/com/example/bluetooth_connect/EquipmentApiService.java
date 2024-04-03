package com.example.bluetooth_connect;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
public interface EquipmentApiService {
    @GET("equipment")
    Call<List<Device>> getAllEquipment();
}
