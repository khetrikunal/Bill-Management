package com.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.billing.repo.CompanyAccessRepository;
import com.billing.repo.SettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CompanyAccessServiceTest {
    @Test
    void deniesCompanyWhenUserHasNoGrant() {
        CompanyAccessRepository grants = mock(CompanyAccessRepository.class);
        SettingsRepository companies = mock(SettingsRepository.class);
        when(companies.existsById(2L)).thenReturn(true);
        when(grants.hasAccess("staff", 2L)).thenReturn(false);
        CompanyAccessService service = new CompanyAccessService(grants, companies);

        ApiException error = assertThrows(ApiException.class, () -> service.requireAccess("staff", 2L));

        assertEquals(HttpStatus.FORBIDDEN, error.status);
    }
}
