package com.billing.service;

import com.billing.dto.Dtos.CompanyOption;
import com.billing.domain.CompanySettings;
import com.billing.domain.UserCompanyAccess;
import com.billing.repo.CompanyAccessRepository;
import com.billing.repo.SettingsRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CompanyAccessService {
    private final CompanyAccessRepository access;
    private final SettingsRepository companies;

    public CompanyAccessService(CompanyAccessRepository access, SettingsRepository companies) {
        this.access = access;
        this.companies = companies;
    }

    public void requireAccess(String username, Long companyId) {
        if (!companies.existsById(companyId)) throw ApiException.notFound("Company not found");
        if (!access.hasAccess(username, companyId)) throw ApiException.forbidden("You are not authorized for this company.");
    }

    public List<Long> companyIds(String username) {
        return access.findCompanyIdsByUsername(username);
    }

    public List<CompanyOption> companies(String username) {
        return companyIds(username).stream().map(companies::findById).flatMap(java.util.Optional::stream)
                .map(s -> new CompanyOption(s.id, s.displayName == null || s.displayName.isBlank()
                        ? s.companyName : s.displayName)).toList();
    }

    public List<CompanyOption> allCompanies() {
        return companies.findAll().stream().map(s -> new CompanyOption(s.id,
                s.displayName == null || s.displayName.isBlank() ? s.companyName : s.displayName)).toList();
    }

    public void grantAll(Long userId) {
        companies.findAll().forEach(company -> grant(userId, company));
    }

    public void replace(Long userId, List<Long> companyIds) {
        List<Long> requested = companyIds == null ? List.of() : companyIds.stream().distinct().toList();
        List<CompanySettings> selected = requested.stream()
                        .map(id -> companies.findById(id).orElseThrow(() -> ApiException.badRequest("Unknown company: " + id)))
                        .toList();
        access.deleteByUserId(userId);
        selected.forEach(company -> grant(userId, company));
    }

    public List<Long> companyIds(Long userId) {
        return access.findByUserIdOrderByCompanyId(userId).stream().map(a -> a.companyId).toList();
    }

    private void grant(Long userId, CompanySettings company) {
        if (access.existsByUserIdAndCompanyId(userId, company.id)) return;
        UserCompanyAccess grant = new UserCompanyAccess();
        grant.userId = userId;
        grant.companyId = company.id;
        access.save(grant);
    }
}
