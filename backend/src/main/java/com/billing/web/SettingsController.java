package com.billing.web;

import com.billing.domain.CompanySettings;
import com.billing.dto.Dtos.SettingsDto;
import com.billing.repo.SettingsRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsRepository repo;

    public SettingsController(SettingsRepository repo) { this.repo = repo; }

    @GetMapping
    @Transactional(readOnly = true)
    public SettingsDto get() { return SettingsDto.of(repo.findById(1L).orElseThrow()); }

    /** Changing settings never touches finalized bills: they carry their own frozen seller snapshot. */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SettingsDto put(@Valid @RequestBody SettingsDto d) {
        CompanySettings s = repo.findById(1L).orElseThrow();
        s.companyName = d.companyName().trim();
        s.address = nz(d.address());
        s.phone = nz(d.phone());
        s.email = nz(d.email());
        s.gstin = nz(d.gstin());
        s.bankName = nz(d.bankName());
        s.bankAccount = nz(d.bankAccount());
        s.bankBranch = nz(d.bankBranch());
        s.ifsc = nz(d.ifsc());
        s.declaration = nz(d.declaration());
        s.signatureText = nz(d.signatureText());
        s.footerNote = nz(d.footerNote());
        s.logo = d.logo() == null || d.logo().isBlank() ? null : d.logo();
        if (s.logo != null && !s.logo.startsWith("data:image/")) {
            throw com.billing.service.ApiException.badRequest("Logo must be an image");
        }
        s.defaultCurrency = d.defaultCurrency();
        s.defaultCgstRate = d.defaultCgstRate();
        s.defaultSgstRate = d.defaultSgstRate();
        s.pageSize = d.pageSize();
        s.invoicePrefix = d.invoicePrefix();
        s.numberPadding = d.numberPadding();
        s.includeFinancialYear = d.includeFinancialYear();
        return SettingsDto.of(repo.save(s));
    }

    private static String nz(String v) { return v == null ? "" : v.trim(); }
}
