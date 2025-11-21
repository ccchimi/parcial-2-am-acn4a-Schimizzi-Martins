package com.app.tasteit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL =
            "https://raw.githubusercontent.com/ccchimi/parcial-2-am-acn4a-Franco_Schimizzi-Melina_Martins/main/";

    private static Retrofit retrofit = null;

    public static RecipesApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(RecipesApiService.class);
    }
}