package com.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bill")
public class Bill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Enumerated(EnumType.STRING) public DocType docType;
    @Enumerated(EnumType.STRING) public BillStatus status = BillStatus.DRAFT;
    public String invoiceNumber;
    public LocalDate invoiceDate;
    public String transactionRef;
    public String poNumber;
    public Long buyerId;
    public String buyerName;
    public String buyerAddress;
    public String buyerGstin;
    public String currency;
    public String sellerSnapshot;
    public String notes;
    public BigDecimal totalTaxable = BigDecimal.ZERO;
    public BigDecimal totalCgst = BigDecimal.ZERO;
    public BigDecimal totalSgst = BigDecimal.ZERO;
    public BigDecimal totalTax = BigDecimal.ZERO;
    public BigDecimal grandTotal = BigDecimal.ZERO;
    public String amountInWords = "";
    public String createdBy;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
    public Instant finalizedAt;
    public Instant cancelledAt;
    public String cancelReason;
    public Long duplicatedFrom;
    @Version public Long version;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    public List<BillItem> items = new ArrayList<>();
}
