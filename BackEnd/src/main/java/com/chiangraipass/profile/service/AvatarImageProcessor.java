package com.chiangraipass.profile.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * Turns an uploaded JPEG or PNG into a small square JPEG. Decoding and re-encoding the picture
 * means only real image data is stored: no metadata, no embedded scripts, and a fixed size.
 */
public final class AvatarImageProcessor {

    public static final int SIZE = 256;
    private static final int MIN_SIDE = 32;
    private static final long MAX_PIXELS = 25_000_000L;

    /** The upload cannot be used as a profile picture; the message is safe to show to the visitor. */
    public static final class InvalidAvatarException extends RuntimeException {
        public InvalidAvatarException(String message) {
            super(message);
        }
    }

    private AvatarImageProcessor() {
    }

    public static byte[] toAvatarJpeg(byte[] upload) {
        if (!isJpeg(upload) && !isPng(upload)) {
            throw new InvalidAvatarException("Choose a JPEG or PNG picture.");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
            if (input == null) {
                throw unreadable();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw unreadable();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < MIN_SIDE || height < MIN_SIDE) {
                    throw new InvalidAvatarException(
                            "The picture is too small. Use one at least " + MIN_SIDE + " x " + MIN_SIDE + " pixels.");
                }
                if ((long) width * height > MAX_PIXELS) {
                    throw new InvalidAvatarException("The picture dimensions are too large.");
                }
                return render(reader.read(0));
            } finally {
                reader.dispose();
            }
        } catch (InvalidAvatarException error) {
            throw error;
        } catch (IOException | RuntimeException error) {
            throw unreadable();
        }
    }

    private static byte[] render(BufferedImage source) throws IOException {
        int side = Math.min(source.getWidth(), source.getHeight());
        BufferedImage square = source.getSubimage(
                (source.getWidth() - side) / 2, (source.getHeight() - side) / 2, side, side);
        Image scaled = square.getScaledInstance(SIZE, SIZE, Image.SCALE_AREA_AVERAGING);
        BufferedImage rgb = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, SIZE, SIZE);
            graphics.drawImage(scaled, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try {
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.85f);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(output);
                writer.write(null, new IIOImage(rgb, null, null), params);
            }
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private static InvalidAvatarException unreadable() {
        return new InvalidAvatarException("That file could not be read as a picture.");
    }

    private static boolean isJpeg(byte[] data) {
        return data.length > 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] data) {
        return data.length > 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
    }
}