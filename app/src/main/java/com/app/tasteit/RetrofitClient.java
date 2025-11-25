package com.app.tasteit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Usamos jsDelivr de cdn para evitar un error reiterativo "429" de GitHub
    private static final String BASE_URL =
            "https://cdn.jsdelivr.net/gh/ccchimi/parcial-2-am-acn4a-Schimizzi-Martins@main/";

    private static Retrofit retrofit = null;

    public static RecipesApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)   // termina en "/"
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(RecipesApiService.class);
    }
}
