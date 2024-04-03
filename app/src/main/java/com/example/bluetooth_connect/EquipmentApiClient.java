package com.example.bluetooth_connect;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class EquipmentApiClient {
    private EquipmentApiService service;

    public EquipmentApiClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://projeto2.ddnsking.com:5001/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(EquipmentApiService.class);
    }

    public Call<List<Device>> getAllEquipment() {
        return service.getAllEquipment();
    }
}
