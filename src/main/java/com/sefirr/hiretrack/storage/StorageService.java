package com.sefirr.hiretrack.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String store(MultipartFile file, String directory);
    Resource load(String path);
    void delete(String path);
}
