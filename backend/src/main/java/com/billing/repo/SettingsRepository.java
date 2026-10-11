package com.billing.repo;

import com.billing.domain.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<CompanySettings, Long> { }
