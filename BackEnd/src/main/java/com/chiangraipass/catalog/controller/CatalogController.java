package com.chiangraipass.catalog.controller;

import com.chiangraipass.catalog.dto.AttractionCatalogItem;
import com.chiangraipass.catalog.dto.PassCatalogItem;
import com.chiangraipass.catalog.service.CatalogService;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/passes")
    public List<PassCatalogItem> passes() {
        return catalogService.findPublishedPasses();
    }

    @GetMapping("/attractions")
    public List<AttractionCatalogItem> attractions() {
        return catalogService.findPublishedAttractions();
    }
}
