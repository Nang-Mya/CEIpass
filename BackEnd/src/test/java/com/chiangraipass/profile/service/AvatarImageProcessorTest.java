package com.chiangraipass.profile.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarImageProcessorTest {

    private static byte[] image(String format, int width, int height) throws Exception {
        BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = picture.createGraphics();
        graphics.setColor(Color.GREEN);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.RED);
        graphics.fillRect(width / 3, height / 3, width / 3, height / 3);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(picture, format, out);
        return out.toByteArray();
    }

    private static boolean looksLikeJpeg(byte[] data) {
        return data.length > 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
    }

    @Test
    void aWideJpegBecomesACroppedSquareJpeg() throws Exception {
        byte[] result = AvatarImageProcessor.toAvatarJpeg(image("jpg", 800, 500));

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(looksLikeJpeg(result)).isTrue();
        assertThat(decoded.getWidth()).isEqualTo(AvatarImageProcessor.SIZE);
        assertThat(decoded.getHeight()).isEqualTo(AvatarImageProcessor.SIZE);
        int centre = decoded.getRGB(128, 128);
        assertThat((centre >> 16) & 0xFF).isGreaterThan(200);
        assertThat(result.length).isLessThan(60_000);
    }

    @Test
    void aPngIsConvertedToJpeg() throws Exception {
        assertThat(looksLikeJpeg(AvatarImageProcessor.toAvatarJpeg(image("png", 300, 300)))).isTrue();
    }

    @Test
    void otherFileTypesAreRefused() throws Exception {
        assertThatThrownBy(() -> AvatarImageProcessor.toAvatarJpeg("not a picture".getBytes()))
                .isInstanceOf(AvatarImageProcessor.InvalidAvatarException.class)
                .hasMessage("Choose a JPEG or PNG picture.");
        assertThatThrownBy(() -> AvatarImageProcessor.toAvatarJpeg(image("gif", 100, 100)))
                .isInstanceOf(AvatarImageProcessor.InvalidAvatarException.class);
        assertThatThrownBy(() -> AvatarImageProcessor.toAvatarJpeg(new byte[0]))
                .isInstanceOf(AvatarImageProcessor.InvalidAvatarException.class);
    }

    @Test
    void aCorruptJpegIsRefused() {
        byte[] corrupt = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5, 6};

        assertThatThrownBy(() -> AvatarImageProcessor.toAvatarJpeg(corrupt))
                .isInstanceOf(AvatarImageProcessor.InvalidAvatarException.class)
                .hasMessage("That file could not be read as a picture.");
    }

    @Test
    void aTinyPictureIsRefused() throws Exception {
        assertThatThrownBy(() -> AvatarImageProcessor.toAvatarJpeg(image("png", 20, 20)))
                .isInstanceOf(AvatarImageProcessor.InvalidAvatarException.class)
                .hasMessageContaining("too small");
    }
}