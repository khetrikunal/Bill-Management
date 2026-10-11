package com.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bill_audit")
public class BillAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long billId;
    public String action;
    public String username;
    public String detail;
    public Instant createdAt = Instant.now();
}
