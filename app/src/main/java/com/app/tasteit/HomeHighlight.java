package com.app.tasteit;

public class HomeHighlight {
    private String title;
    private String description;
    private String imageUrl;

    public HomeHighlight(String title, String description, String imageUrl) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
