package com.miniapi.router.core.spi;

import com.miniapi.router.core.domain.RequestLogMeta;

public interface LogRepository {
    void save(RequestLogMeta meta);
    RequestLogMeta findById(Long id);
}
