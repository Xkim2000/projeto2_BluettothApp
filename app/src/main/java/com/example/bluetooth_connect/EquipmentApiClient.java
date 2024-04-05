package com.example.bluetooth_connect;

import android.service.autofill.UserData;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okio.Timeout;
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

    // Método para inserir dados do usuário
    public Call<Integer> insertUserData(ArrayList<Record> records) {
        return service.insertUserData(records);
    }



}
