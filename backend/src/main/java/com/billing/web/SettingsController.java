package com.billing.web;

import com.billing.domain.CompanySettings;
import com.billing.dto.Dtos.SettingsDto;
import com.billing.repo.SettingsRepository;
<<<<<<< HEAD
import com.billing.service.ApiException;
import com.billing.service.CompanyAccessService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
=======
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
>>>>>>> origin/main

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsRepository repo;
<<<<<<< HEAD
    private final CompanyAccessService companyAccess;

    public SettingsController(SettingsRepository repo, CompanyAccessService companyAccess) {
        this.repo = repo;
        this.companyAccess = companyAccess;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public SettingsDto get(@RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId,
                           Authentication authentication) {
        long id = companyId(companyId);
        companyAccess.requireAccess(authentication.getName(), id);
        return SettingsDto.of(loadCompany(id));
    }

    @GetMapping("/companies")
    @Transactional(readOnly = true)
    public List<com.billing.dto.Dtos.CompanyOption> companies(Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return companyAccess.allCompanies();
        }
        return companyAccess.companies(authentication.getName());
    }
=======

    public SettingsController(SettingsRepository repo) { this.repo = repo; }

    @GetMapping
    @Transactional(readOnly = true)
    public SettingsDto get() { return SettingsDto.of(repo.findById(1L).orElseThrow()); }
>>>>>>> origin/main

    /** Changing settings never touches finalized bills: they carry their own frozen seller snapshot. */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
<<<<<<< HEAD
    public SettingsDto put(@RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId,
                           @Valid @RequestBody SettingsDto d, Authentication authentication) {
        long id = companyId(companyId);
        companyAccess.requireAccess(authentication.getName(), id);
        CompanySettings s = loadCompany(id);
        s.companyName = d.companyName().trim();
        s.displayName = nz(d.displayName());
        s.address = nz(d.address());
        s.billingAddress = nz(d.billingAddress());
        s.phone = nz(d.phone());
        s.email = nz(d.email());
        s.gstin = nz(d.gstin());
        s.pan = nz(d.pan());
=======
    public SettingsDto put(@Valid @RequestBody SettingsDto d) {
        CompanySettings s = repo.findById(1L).orElseThrow();
        s.companyName = d.companyName().trim();
        s.address = nz(d.address());
        s.phone = nz(d.phone());
        s.email = nz(d.email());
        s.gstin = nz(d.gstin());
>>>>>>> origin/main
        s.bankName = nz(d.bankName());
        s.bankAccount = nz(d.bankAccount());
        s.bankBranch = nz(d.bankBranch());
        s.ifsc = nz(d.ifsc());
<<<<<<< HEAD
        s.upiId = nz(d.upiId());
        s.defaultPaymentTerms = nz(d.defaultPaymentTerms());
        s.declaration = nz(d.declaration());
        s.signatureText = nz(d.signatureText());
        s.authorizedSignatory = nz(d.authorizedSignatory());
        s.signatoryDesignation = nz(d.signatoryDesignation());
=======
        s.declaration = nz(d.declaration());
        s.signatureText = nz(d.signatureText());
>>>>>>> origin/main
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
<<<<<<< HEAD

    private CompanySettings loadCompany(long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Company not found"));
    }

    private static long companyId(String key) {
        try { return Long.parseLong(key); }
        catch (NumberFormatException e) { throw ApiException.badRequest("Invalid company selection."); }
    }
=======
>>>>>>> origin/main
}
