package com.chiangraipass.persistence.repository;

import java.util.List;

import com.chiangraipass.persistence.entity.AttractionCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttractionCatalogJpaRepository extends JpaRepository<AttractionCatalogEntity, String> {

    List<AttractionCatalogEntity> findByPublishedTrueOrderByDisplayOrderAscPartnerIdAsc();
}
