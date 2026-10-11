package com.billing.repo;

import com.billing.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findTop200ByNameContainingIgnoreCaseOrderByName(String q);
}
