package com.chiangraipass.api.catalog;

import java.util.List;

import com.chiangraipass.persistence.entity.AttractionCatalogEntity;
import com.chiangraipass.persistence.entity.PassCatalogEntity;
import com.chiangraipass.persistence.repository.AttractionCatalogJpaRepository;
import com.chiangraipass.persistence.repository.PassCatalogJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {

    private final PassCatalogJpaRepository passes;
    private final AttractionCatalogJpaRepository attractions;

    public CatalogRepository(
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
