package com.billing.repo;

import com.billing.domain.BillAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillAuditRepository extends JpaRepository<BillAudit, Long> {
    List<BillAudit> findByBillIdOrderByCreatedAtAscIdAsc(Long billId);
}
