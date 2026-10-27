package com.chiangraipass.profile.repository;

import java.util.UUID;

import com.chiangraipass.profile.entity.VisitorAvatarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorAvatarRepository extends JpaRepository<VisitorAvatarEntity, UUID> {
}