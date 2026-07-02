package com.example.freshfarm3.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    // AtomicLong ensures thread-safety in concurrent requests
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * Generates an order number in the format: FF-YYYYMMDD-NNNNNN
     * Example: FF-20240615-000001
     */
    public String generate() {
        String date     = LocalDate.now().format(DATE_FORMAT);
        long   seq      = sequence.incrementAndGet();
        return String.format("FF-%s-%06d", date, seq);
    }
}