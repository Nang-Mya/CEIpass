package com.chiangraipass.persistence.repository;

import java.util.List;

import com.chiangraipass.persistence.entity.PassCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassCatalogJpaRepository extends JpaRepository<PassCatalogEntity, String> {

    List<PassCatalogEntity> findByPublishedTrueOrderByDisplayOrderAscPassIdAsc();
}
