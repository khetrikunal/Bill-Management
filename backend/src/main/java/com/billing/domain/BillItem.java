package com.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bill_item")
public class BillItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bill_id") public Bill bill;
    public int lineNo;
    public String poNumber;
    public String name;
    public String itemCode;
    public String hsn;
    public BigDecimal qty;
    public String unit;
    public BigDecimal rate;
    public BigDecimal cgstRate;
    public BigDecimal sgstRate;
    public BigDecimal taxable = BigDecimal.ZERO;
    public BigDecimal cgst = BigDecimal.ZERO;
    public BigDecimal sgst = BigDecimal.ZERO;
    public BigDecimal total = BigDecimal.ZERO;
}
