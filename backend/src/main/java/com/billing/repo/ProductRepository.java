package com.billing.repo;

import com.billing.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findTop500ByNameContainingIgnoreCaseOrderByName(String q);
    List<Product> findByItemCodeIgnoreCase(String itemCode);
    List<Product> findByNameIgnoreCaseAndHsn(String name, String hsn);
}
