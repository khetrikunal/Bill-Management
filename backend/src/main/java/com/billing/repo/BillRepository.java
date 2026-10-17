package com.billing.repo;

import com.billing.domain.Bill;
import com.billing.domain.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {
    long countByStatus(BillStatus status);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
    List<Bill> findTop5ByOrderByCreatedAtDesc();
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndStatus(Long companyId, BillStatus status);
    long countByCompanyIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long companyId, Instant from, Instant to);
    List<Bill> findTop5ByCompanyIdOrderByCreatedAtDesc(Long companyId);
    @Query("select count(b) from Bill b where b.status = com.billing.domain.BillStatus.FINAL "
            + "and b.dueDate < :date and b.grandTotal > coalesce((select sum(p.amount) from BillPayment p "
            + "where p.bill.id = b.id), 0)")
    long countOverdue(LocalDate date);

    @Query("select count(b) from Bill b where b.companyId = :companyId "
            + "and b.status = com.billing.domain.BillStatus.FINAL and b.dueDate < :date "
            + "and b.grandTotal > coalesce((select sum(p.amount) from BillPayment p where p.bill.id = b.id), 0)")
    long countOverdueByCompany(Long companyId, LocalDate date);

    @Query("select b.currency, sum(b.grandTotal) from Bill b where b.status = com.billing.domain.BillStatus.FINAL "
            + "and b.invoiceDate >= :from and b.invoiceDate < :to group by b.currency")
    List<Object[]> finalizedTotals(LocalDate from, LocalDate to);

    @Query("select b.currency, sum(b.grandTotal) from Bill b where b.companyId = :companyId "
            + "and b.status = com.billing.domain.BillStatus.FINAL and b.invoiceDate >= :from "
            + "and b.invoiceDate < :to group by b.currency")
    List<Object[]> finalizedTotalsByCompany(Long companyId, LocalDate from, LocalDate to);

    @Query("select b.currency, sum(b.grandTotal) from Bill b where b.companyId = :companyId "
            + "and b.status = com.billing.domain.BillStatus.FINAL group by b.currency")
    List<Object[]> billedTotalsByCompany(Long companyId);
}
