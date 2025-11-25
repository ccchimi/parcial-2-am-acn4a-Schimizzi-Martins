package com.app.tasteit;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RecipesApiService {
    @GET("recipes.json")
    Call<List<Recipe>> getRecipes();
}
