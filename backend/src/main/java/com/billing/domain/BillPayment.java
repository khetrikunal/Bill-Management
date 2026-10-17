package com.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bill_payment")
public class BillPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bill_id", nullable = false) public Bill bill;
    public BigDecimal amount;
    public LocalDate paymentDate;
    public String method;
    public String transactionReference;
    public String notes;
    public String recordedBy;
    public Instant createdAt = Instant.now();
}
