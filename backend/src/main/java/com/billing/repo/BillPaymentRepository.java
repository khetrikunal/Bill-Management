package com.billing.repo;

import com.billing.domain.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
    List<BillPayment> findByBillIdOrderByPaymentDateAscIdAsc(Long billId);

    @Query("select p.bill.currency, sum(p.amount) from BillPayment p where p.bill.companyId = :companyId "
            + "and p.bill.status = com.billing.domain.BillStatus.FINAL group by p.bill.currency")
    List<Object[]> receivedTotalsByCompany(Long companyId);
}
