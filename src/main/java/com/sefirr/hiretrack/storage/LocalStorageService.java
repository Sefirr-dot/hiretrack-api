package com.sefirr.hiretrack.storage;

import com.sefirr.hiretrack.exception.InvalidFileException;
import com.sefirr.hiretrack.exception.StorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    /**
     * Allowed content types and the extension used on disk. The extension comes from this map,
     * never from the client's filename, so a file can't be smuggled in as e.g. ".html".
     */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", ".pdf",
            "application/msword", ".doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
            "image/png", ".png",
            "image/jpeg", ".jpg"
    );

    @Value("${storage.upload-dir:uploads/}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String directory) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Cannot store empty file");
        }
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new InvalidFileException("File type not allowed: " + file.getContentType()
                    + ". Allowed: PDF, DOC, DOCX, PNG, JPG");
        }

        String filename = UUID.randomUUID() + extension;
        Path dest = resolveSafely(directory + "/" + filename);

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(dest.getParent());
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }

        return directory + "/" + filename;
    }

    @Override
    public Resource load(String path) {
        try {
            Resource resource = new UrlResource(resolveSafely(path).toUri());
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
            Path file = resolveSafely(path);
            Files.deleteIfExists(file);
            // Remove the per-application directory once it is empty
            Path parent = file.getParent();
            if (!parent.equals(rootLocation)) {
                try (var entries = Files.list(parent)) {
                    if (entries.findAny().isEmpty()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
        } catch (IOException e) {
            throw new StorageException("Could not delete file: " + path, e);
        }
    }

    private Path resolveSafely(String path) {
        Path resolved = rootLocation.resolve(path).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new StorageException("Path outside storage directory: " + path);
        }
        return resolved;
    }
}
