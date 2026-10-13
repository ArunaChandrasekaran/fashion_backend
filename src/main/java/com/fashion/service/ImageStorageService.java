package com.fashion.service;

import com.fashion.config.FashionProperties;
import com.fashion.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> MAGIC_EXT = Map.of(
            "ffd8ff", ".jpg",
            "89504e47", ".png",
            "52494646", ".webp"
    );

    private final FashionProperties properties;

    public ImageStorageService(FashionProperties properties) {
        this.properties = properties;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.validation("Please choose an image file");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw ApiException.validation("Images must be 2 MB or smaller");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw ApiException.validation("Only JPEG, PNG, and WebP images are allowed");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(12);
            String ext = extensionFor(header, contentType);
            Path dir = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw ApiException.badRequest("Unable to store the uploaded image");
        }
    }

    private String extensionFor(byte[] header, String contentType) {
        String hex = toHex(header);
        if (hex.startsWith("ffd8ff") && contentType.equals("image/jpeg")) {
            return ".jpg";
        }
        if (hex.startsWith("89504e47") && contentType.equals("image/png")) {
            return ".png";
        }
        if (hex.startsWith("52494646") && hex.contains("57454250") && contentType.equals("image/webp")) {
            return ".webp";
        }
        throw ApiException.validation("File content does not match a supported image type");
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
