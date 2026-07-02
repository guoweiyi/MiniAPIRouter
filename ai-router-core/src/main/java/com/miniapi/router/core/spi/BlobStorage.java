package com.miniapi.router.core.spi;

public interface BlobStorage {
    String store(String path, String content);
    String read(String path);
    void delete(String path);
}
