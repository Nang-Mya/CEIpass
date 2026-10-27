package com.chiangraipass.profile.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.profile.entity.VisitorAvatarEntity;
import com.chiangraipass.profile.repository.VisitorAvatarRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvatarService {

    /** Largest upload accepted before the picture is decoded and shrunk. */
    public static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;

    private final VisitorAvatarRepository avatars;

    public AvatarService(VisitorAvatarRepository avatars) {
        this.avatars = avatars;
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> find(UUID accountId) {
        return avatars.findById(accountId).map(VisitorAvatarEntity::getImageData);
    }

    /** Replaces the visitor's picture with a square JPEG made from the upload. */
    @Transactional
    public void save(UUID accountId, byte[] upload) {
        byte[] jpeg;
        try {
            jpeg = AvatarImageProcessor.toAvatarJpeg(upload);
        } catch (AvatarImageProcessor.InvalidAvatarException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage());
        }
        avatars.save(new VisitorAvatarEntity(accountId, jpeg, Instant.now()));
    }
}