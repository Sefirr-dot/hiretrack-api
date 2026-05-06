package com.sefirr.hiretrack.storage;

import com.sefirr.hiretrack.exception.StorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg"
    );

    @Value("${storage.upload-dir:uploads/}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String directory) {
        if (file.isEmpty()) {
            throw new StorageException("Cannot store empty file");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new StorageException("File type not allowed: " + file.getContentType());
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String filename = UUID.randomUUID() + extension;
        Path dest = rootLocation.resolve(directory).resolve(filename).normalize();

        if (!dest.startsWith(rootLocation)) {
            throw new StorageException("Cannot store file outside designated directory");
        }

        try {
            Files.createDirectories(dest.getParent());
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }

        return directory + "/" + filename;
    }

    @Override
    public Resource load(String path) {
        try {
            Path file = rootLocation.resolve(path).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new StorageException("Could not read file: " + path);
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path file = rootLocation.resolve(path).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Could not delete file: " + path, e);
        }
    }
}
