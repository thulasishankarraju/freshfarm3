package com.example.freshfarm3.service;

import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.ProductImage;
import com.example.freshfarm3.exception.ValidationException;
import com.example.freshfarm3.repository.ProductImageRepository;
import com.example.freshfarm3.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * FileUploadService — Stores uploaded files (product photos, farmer documents,
 * profile pictures, etc.) on local disk under {@code app.upload.dir} and
 * returns a public URL served from {@code app.upload.base-url} (wired up in
 * WebConfig + permitted in SecurityConfig).
 *
 * Swap the internals for an S3 / Cloudinary client later without touching
 * any of the callers — they only depend on this class's public methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class  FileUploadService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository      productRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:/uploads}")
    private String baseUrl;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB (raised from 5MB)

    // Expanded to cover the image formats farmers are likely to actually
    // upload from phones/cameras — was previously JPEG/PNG/WEBP only, which
    // rejected GIF, BMP, TIFF, and HEIC/HEIF (common on iPhones) with a
    // "Unsupported file type" ValidationException.
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp",
            "image/gif", "image/bmp", "image/tiff", "image/heic", "image/heif"
    );

    // ─────────────────────────────────────────────────────────────────────────
    //  UPLOAD A SINGLE FILE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates and saves one file to disk, returning the URL clients should
     * use to fetch it (e.g. {@code /uploads/2f1a...c9.jpg}).
     */
    public String uploadFile(MultipartFile file) {
        validateFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String filename   = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path target = uploadPath.resolve(filename).normalize();
            if (!target.getParent().equals(uploadPath.normalize())) {
                // Defends against a crafted filename escaping the upload dir.
                throw new ValidationException("Invalid file name");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = baseUrl + "/" + filename;
            log.info("Stored file '{}' ({} bytes) -> {}", file.getOriginalFilename(), file.getSize(), url);
            return url;

        } catch (IOException e) {
            log.error("Failed to store file '{}'", file.getOriginalFilename(), e);
            throw new ValidationException("Could not store file: " + file.getOriginalFilename());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPLOAD MULTIPLE FILES (returns URLs only, no persistence)
    // ─────────────────────────────────────────────────────────────────────────

    public List<String> uploadFiles(List<MultipartFile> files) {
        return files.stream().map(this::uploadFile).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPLOAD + ATTACH PRODUCT IMAGES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uploads each file and persists a {@link ProductImage} row linked to the
     * given product. The first image saved becomes the primary image if the
     * product doesn't already have one.
     *
     * Only {@code product.getId()} is required on the incoming {@code product}
     * argument — a managed reference is looked up internally so callers can
     * safely pass a lightweight/detached instance (e.g. from a controller).
     */
    @Transactional
    public List<ProductImage> uploadProductImages(List<MultipartFile> images, Product product) {
        if (product == null || product.getId() == null) {
            throw new ValidationException("A saved product is required before uploading images");
        }
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        Product productRef = productRepository.getReferenceById(product.getId());
        List<ProductImage> existing = productImageRepository.findByProductOrderByDisplayOrderAsc(productRef);
        boolean hasPrimaryAlready = existing.stream().anyMatch(ProductImage::getIsPrimary);
        int nextOrder = existing.size();

        for (int i = 0; i < images.size(); i++) {
            String url = uploadFile(images.get(i));
            ProductImage image = ProductImage.builder()
                    .imageUrl(url)
                    .product(productRef)
                    .displayOrder(nextOrder + i + 1)
                    .isPrimary(false)
                    .build();
            ProductImage saved = productImageRepository.save(image);
            log.debug("Attached image id={} to product id={}", saved.getId(), product.getId());
        }

        List<ProductImage> all = productImageRepository.findByProductOrderByDisplayOrderAsc(productRef);
        if (!hasPrimaryAlready && !all.isEmpty()) {
            ProductImage first = all.get(0);
            first.setIsPrimary(true);
            productImageRepository.save(first);
        }
        return all;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes the file backing a previously-returned URL from disk.
     * Returns true if a file was found and removed.
     */
    public boolean deleteFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return false;
        }
        String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path target = Paths.get(uploadDir).resolve(filename).normalize();

        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.info("Deleted file: {}", target);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete file '{}'", target, e);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File exceeds the 10MB size limit: " + file.getOriginalFilename());
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("Unsupported file type (allowed: JPEG, PNG, WEBP, GIF, BMP, TIFF, HEIC): " + contentType);
        }
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
    }
}