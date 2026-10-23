package com.council.tenderapp.technicalservices.logging;

import java.time.Instant;

public record LogEntry(Instant timestamp, String level, String message) {}
