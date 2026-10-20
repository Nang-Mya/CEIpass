package com.chiangraipass.api.home;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class HomeController {

    @GetMapping("/home")
    public ResponseEntity<HomeResponseDTO> getHomeData() {
        List<BannerDTO> banners = Arrays.asList(
            new BannerDTO(1L, "/images/banner1.jpg", "Discover Chiang Rai"),
            new BannerDTO(2L, "/images/banner2.jpg", "Temples & Culture")
        );

        List<PackageDTO> packages = Arrays.asList(
            new PackageDTO(1L, "1-Day Pass", 499.00, "Access for 1 day"),
            new PackageDTO(2L, "3-Day Pass", 1200.00, "Access for 3 days"),
            new PackageDTO(3L, "5-Day Pass", 1800.00, "Access for 5 days")
        );

        List<RecommendationDTO> recommendations = Arrays.asList(
            new RecommendationDTO(1L, "Place 1: Wat Rong Khun", "/images/place1.jpg", "White Temple architecture"),
            new RecommendationDTO(2L, "Place 2: Wat Huay Pla Kang", "/images/place2.jpg", "Big Buddha statue"),
            new RecommendationDTO(3L, "Place 3: Singha Park", "/images/place3.jpg", "Nature and farm views")
        );

        HomeResponseDTO response = new HomeResponseDTO(banners, packages, recommendations);
        return ResponseEntity.ok(response);
    }
}