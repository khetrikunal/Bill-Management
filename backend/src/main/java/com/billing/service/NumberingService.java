package com.billing.service;

import com.billing.domain.CompanySettings;
import com.billing.domain.NumberSequence;
import com.billing.repo.NumberSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/**
 * Final invoice numbers are allocated only at finalization, inside the finalize transaction, using a row lock
 * on the sequence row (SELECT ... FOR UPDATE). Concurrent finalizations queue on that lock, so numbers are
 * unique and gap-free. A company/invoice unique index is a second safety net.
 * Drafts have no invoice number. A rolled-back finalization also rolls back its sequence increment.
 */
@Service
public class NumberingService {
    private final NumberSequenceRepository repo;

    public NumberingService(NumberSequenceRepository repo) { this.repo = repo; }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(CompanySettings s, LocalDate invoiceDate) {
        String fy = financialYear(invoiceDate);
        String key = s.id + "|" + s.invoicePrefix + "|" + (s.includeFinancialYear ? fy : "-");
        repo.insertIfAbsent(key);
        NumberSequence seq = repo.lockByKey(key).orElseThrow();
        seq.lastValue += 1;
        repo.save(seq);
        return format(s.invoicePrefix, fy, seq.lastValue, s.numberPadding, s.includeFinancialYear);
    }

    /** Indian financial year: 1 April to 31 March, e.g. "2026-27". */
    public static String financialYear(LocalDate d) {
        int start = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        return start + "-" + String.format("%02d", (start + 1) % 100);
    }

    public static String format(String prefix, String fy, long n, int padding, boolean withFy) {
        String num = String.format("%0" + padding + "d", n);
        return withFy ? prefix + "-" + fy + "-" + num : prefix + "-" + num;
    }
}
