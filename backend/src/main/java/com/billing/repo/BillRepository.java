package com.billing.repo;

import com.billing.domain.Bill;
import com.billing.domain.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {
    long countByStatus(BillStatus status);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
    List<Bill> findTop5ByOrderByCreatedAtDesc();

    @Query("select b.currency, sum(b.grandTotal) from Bill b where b.status = com.billing.domain.BillStatus.FINAL "
            + "and b.invoiceDate >= :from and b.invoiceDate < :to group by b.currency")
    List<Object[]> finalizedTotals(LocalDate from, LocalDate to);
}
