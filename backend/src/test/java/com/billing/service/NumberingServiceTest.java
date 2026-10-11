package com.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NumberingServiceTest {
    @Test void financialYearStartsInApril() {
        assertEquals("2026-27", NumberingService.financialYear(LocalDate.of(2026, 4, 1)));
        assertEquals("2025-26", NumberingService.financialYear(LocalDate.of(2026, 3, 31)));
        assertEquals("1999-00", NumberingService.financialYear(LocalDate.of(1999, 12, 1)));
    }

    @Test void formatsWithAndWithoutFinancialYear() {
        assertEquals("SSE-2026-27-0001", NumberingService.format("SSE", "2026-27", 1, 4, true));
        assertEquals("SSE-0042", NumberingService.format("SSE", "2026-27", 42, 4, false));
        assertEquals("EXP-2026-27-12345", NumberingService.format("EXP", "2026-27", 12345, 4, true));
    }
}
