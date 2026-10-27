package com.chiangraipass.profile.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.profile.service.AvatarService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** The signed-in visitor's own profile picture. */
@RestController
@RequestMapping("/api/v1/me/avatar")
public class AvatarController {

    private final AvatarService avatars;

    public AvatarController(AvatarService avatars) {
        this.avatars = avatars;
    }

    /** 200 with the JPEG, or 204 when the visitor has not set a picture. */
    @GetMapping
    public ResponseEntity<byte[]> current(@AuthenticationPrincipal VisitorIdentity identity) {
        Optional<byte[]> image = avatars.find(identity.accountId());
        if (image.isEmpty()) {
            return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.get());
    }

    /** The request body is the raw picture bytes (JPEG or PNG). */
    @PutMapping
    public ResponseEntity<Map<String, String>> replace(
            @AuthenticationPrincipal VisitorIdentity identity, HttpServletRequest request) throws IOException {
        byte[] upload = request.getInputStream().readNBytes(AvatarService.MAX_UPLOAD_BYTES + 1);
        if (upload.length > AvatarService.MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "The picture is too large. Choose one under 2 MB.");
        }
        avatars.save(identity.accountId(), upload);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("message", "Profile picture updated."));
    }
}