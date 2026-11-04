package com.chiangraipass.pass.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.chiangraipass.pass.dto.QrPassResponse;
import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PassQrService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final int QR_SIZE = 320;

    private final UserPassJpaRepository userPassRepository;

    public PassQrService(UserPassJpaRepository userPassRepository) {
        this.userPassRepository = userPassRepository;
    }

    @Transactional
    public QrPassResponse issueQrForAccount(UUID accountId) {
        return issueQrForAccount(accountId, null);
    }

    @Transactional
    public QrPassResponse issueQrForAccount(UUID accountId, UUID userPassId) {
        List<UserPassEntity> ownedPasses = userPassRepository.findByAccountIdWithPass(accountId);
        if (ownedPasses.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No pass found. Please buy a pass first.");
        }

        LocalDate today = LocalDate.now(ZONE);
        UserPassEntity activePass = ownedPasses.stream()
                .filter(pass -> userPassId == null || userPassId.equals(pass.getUserPassId()))
                .filter(pass -> isActiveToday(pass, today))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        userPassId == null ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT,
                        userPassId == null
                                ? "You do not have an active pass for today."
                                : "This pass is not active today."));

        if (activePass.getQrToken() == null) {
            activePass.ensureQrToken();
            userPassRepository.save(activePass);
        }
        String payload = "CEIPASS:2:" + activePass.getQrToken();
        String qrImageDataUrl = generateQrDataUrl(payload);

        return new QrPassResponse(
                accountId,
                activePass.getUserPassId(),
                activePass.getPass().getDisplayName(),
                activePass.getHolderName(),
                qrImageDataUrl,
                payload
        );
    }

    private boolean isActiveToday(UserPassEntity pass, LocalDate today) {
        return "ACTIVE".equalsIgnoreCase(pass.getStatus())
                && (pass.getStartDate() == null || !today.isBefore(pass.getStartDate()))
                && (pass.getEndDate() == null || !today.isAfter(pass.getEndDate()));
    }

    private String generateQrDataUrl(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate QR code image.", e);
        }
    }
}