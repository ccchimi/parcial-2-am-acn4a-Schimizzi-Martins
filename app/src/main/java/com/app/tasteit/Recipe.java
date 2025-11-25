package com.app.tasteit;

public class Recipe {
    private String title;
    private String category;
    private String description;
    private String imageUrl;
    private String cookingTime;

    public Recipe() {
    }

    // Constructor completo (para Retrofit / JSON / usos nuevos)
    public Recipe(String title, String category, String description, String imageUrl, String cookingTime) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.cookingTime = cookingTime;
    }

    // Constructor viejo (para no romper favoritos, detalle, etc.)
    public Recipe(String title, String description, String imageUrl, String cookingTime) {
        this(title, null, description, imageUrl, cookingTime);
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCookingTime() {
        return cookingTime;
    }
}
