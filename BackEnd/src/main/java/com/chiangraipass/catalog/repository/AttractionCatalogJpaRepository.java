package com.chiangraipass.catalog.repository;

import java.util.List;

import com.chiangraipass.catalog.entity.AttractionCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttractionCatalogJpaRepository extends JpaRepository<AttractionCatalogEntity, String> {

    List<AttractionCatalogEntity> findByPublishedTrueOrderByDisplayOrderAscPartnerIdAsc();
}
