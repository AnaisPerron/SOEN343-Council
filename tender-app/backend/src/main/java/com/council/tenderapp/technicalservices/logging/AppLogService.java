package com.council.tenderapp.technicalservices.logging;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AppLogService {

    private static final Logger logger = LoggerFactory.getLogger(AppLogService.class);

    public void info(String message) {
        LogEntry entry = new LogEntry(Instant.now(), "INFO", message);
        logger.info(entry.message());
    }

    public void error(String message, Throwable cause) {
        LogEntry entry = new LogEntry(Instant.now(), "ERROR", message);
        logger.error(entry.message(), cause);
    }
}
