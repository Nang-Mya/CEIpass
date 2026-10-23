package com.chiangraipass.catalog.repository;

import java.util.List;

import com.chiangraipass.catalog.entity.PassCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassCatalogJpaRepository extends JpaRepository<PassCatalogEntity, String> {

    List<PassCatalogEntity> findByPublishedTrueOrderByDisplayOrderAscPassIdAsc();
}
