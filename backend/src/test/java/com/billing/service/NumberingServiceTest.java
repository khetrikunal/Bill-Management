package com.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.billing.domain.CompanySettings;
import com.billing.domain.NumberSequence;
import com.billing.repo.NumberSequenceRepository;
import java.time.LocalDate;
import java.util.Optional;
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
    @Test void sequenceKeysAreIndependentForEachCompany() {
        NumberSequenceRepository repo = mock(NumberSequenceRepository.class);
        when(repo.lockByKey(anyString())).thenAnswer(invocation -> Optional.of(new NumberSequence()));
        when(repo.save(any(NumberSequence.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NumberingService numbering = new NumberingService(repo);
        CompanySettings sset = company(1L);
        CompanySettings pk = company(2L);

        assertEquals("INV-2026-27-0001", numbering.next(sset, LocalDate.of(2026, 4, 1)));
        assertEquals("INV-2026-27-0001", numbering.next(pk, LocalDate.of(2026, 4, 1)));
        verify(repo).insertIfAbsent("1|INV|2026-27");
        verify(repo).insertIfAbsent("2|INV|2026-27");
    }

    private static CompanySettings company(long id) {
        CompanySettings settings = new CompanySettings();
        settings.id = id;
        settings.invoicePrefix = "INV";
        settings.numberPadding = 4;
        settings.includeFinancialYear = true;
        return settings;
    }
}
