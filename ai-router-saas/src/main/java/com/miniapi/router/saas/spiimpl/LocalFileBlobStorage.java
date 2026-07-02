package com.miniapi.router.saas.spiimpl;

import com.miniapi.router.core.spi.BlobStorage;
import com.miniapi.router.core.config.CoreProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LocalFileBlobStorage implements BlobStorage {

    private final Path baseDir;

    public LocalFileBlobStorage(CoreProperties properties) {
        this.baseDir = Paths.get(properties.getBlobStoragePath());
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create blob storage dir", e);
        }
    }

    @Override
    public String store(String path, String content) {
        try {
            Path full = baseDir.resolve(path);
            Files.createDirectories(full.getParent());
            Files.writeString(full, content);
            return path;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store blob: " + path, e);
        }
    }

    @Override
    public String read(String path) {
        try {
            Path full = baseDir.resolve(path);
            if (!Files.exists(full)) return null;
            return Files.readString(full);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path full = baseDir.resolve(path);
            Files.deleteIfExists(full);
        } catch (IOException ignored) {
        }
    }
}
