package com.billing.repo;

import com.billing.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
<<<<<<< HEAD
    List<Product> findTop500ByCompanyIdAndNameContainingIgnoreCaseOrderByName(Long companyId, String q);
    List<Product> findByCompanyIdAndItemCodeIgnoreCase(Long companyId, String itemCode);
    List<Product> findByCompanyIdAndNameIgnoreCaseAndHsn(Long companyId, String name, String hsn);
    int deleteByIdAndCompanyId(Long id, Long companyId);
=======
    List<Product> findTop500ByNameContainingIgnoreCaseOrderByName(String q);
    List<Product> findByItemCodeIgnoreCase(String itemCode);
    List<Product> findByNameIgnoreCaseAndHsn(String name, String hsn);
>>>>>>> origin/main
}
