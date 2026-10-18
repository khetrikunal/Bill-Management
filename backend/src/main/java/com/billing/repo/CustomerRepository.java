package com.billing.repo;

import com.billing.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findTop200ByCompanyIdAndNameContainingIgnoreCaseOrderByName(Long companyId, String q);
    int deleteByIdAndCompanyId(Long id, Long companyId);
}
