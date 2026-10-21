package com.chiangraipass.catalog.service;

import java.util.List;

import com.chiangraipass.catalog.dto.AttractionCatalogItem;
import com.chiangraipass.catalog.dto.PassCatalogItem;
import com.chiangraipass.catalog.entity.AttractionCatalogEntity;
import com.chiangraipass.catalog.entity.PassCatalogEntity;
import com.chiangraipass.catalog.repository.AttractionCatalogJpaRepository;
import com.chiangraipass.catalog.repository.PassCatalogJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final PassCatalogJpaRepository passes;
    private final AttractionCatalogJpaRepository attractions;

    public CatalogService(
            PassCatalogJpaRepository passes,
            AttractionCatalogJpaRepository attractions) {
        this.passes = passes;
        this.attractions = attractions;
    }

    public List<PassCatalogItem> findPublishedPasses() {
        return passes.findByPublishedTrueOrderByDisplayOrderAscPassIdAsc().stream()
                .map(PassCatalogEntity::toCatalogItem)
                .toList();
    }

    public List<AttractionCatalogItem> findPublishedAttractions() {
        return attractions.findByPublishedTrueOrderByDisplayOrderAscPartnerIdAsc().stream()
                .map(AttractionCatalogEntity::toCatalogItem)
                .toList();
    }
}
