package com.sefirr.hiretrack.storage;

import com.sefirr.hiretrack.exception.InvalidFileException;
import com.sefirr.hiretrack.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new LocalStorageService();
        ReflectionTestUtils.setField(storage, "uploadDir", tempDir.toString());
        storage.init();
    }

    @Test
    void store_usesExtensionFromContentType_notFromClientFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.html", "application/pdf", "%PDF".getBytes());

        String path = storage.store(file, "app-1");

        assertThat(path).startsWith("app-1/").endsWith(".pdf");
        assertThat(Files.exists(tempDir.resolve(path))).isTrue();
    }

    @Test
    void store_rejectsDisallowedType() {
        MockMultipartFile file = new MockMultipartFile("file", "x.html", "text/html", "<script>".getBytes());

        assertThatThrownBy(() -> storage.store(file, "app-1")).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void store_rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storage.store(file, "app-1")).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void load_rejectsPathTraversal() {
        assertThatThrownBy(() -> storage.load("../../etc/passwd")).isInstanceOf(StorageException.class);
    }

    @Test
    void delete_removesFileAndEmptyDirectory() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF".getBytes());
        String path = storage.store(file, "app-2");

        storage.delete(path);

        assertThat(Files.exists(tempDir.resolve(path))).isFalse();
        assertThat(Files.exists(tempDir.resolve("app-2"))).isFalse();
    }
}
