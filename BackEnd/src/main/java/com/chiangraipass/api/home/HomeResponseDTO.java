package com.chiangraipass.api.home;

import java.util.List;

public class HomeResponseDTO {
    private List<BannerDTO> banners;
    private List<PackageDTO> packages;
    private List<RecommendationDTO> recommendations;

    public HomeResponseDTO(List<BannerDTO> banners, List<PackageDTO> packages, List<RecommendationDTO> recommendations) {
        this.banners = banners;
        this.packages = packages;
        this.recommendations = recommendations;
    }

    public List<BannerDTO> getBanners() { return banners; }
    public List<PackageDTO> getPackages() { return packages; }
    public List<RecommendationDTO> getRecommendations() { return recommendations; }
}

class BannerDTO {
    private Long id;
    private String imageUrl;
    private String title;

    public BannerDTO(Long id, String imageUrl, String title) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
    }
    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getTitle() { return title; }
}

class PackageDTO {
    private Long id;
    private String name; // รองรับ 1 วัน, 3 วัน, 5 วัน
    private double price;
    private String description;

    public PackageDTO(Long id, String name, double price, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
}

class RecommendationDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String description;

    public RecommendationDTO(Long id, String name, String imageUrl, String description) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.description = description;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
}