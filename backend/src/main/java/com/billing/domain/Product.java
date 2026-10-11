package com.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    public String itemCode;
    public String hsn;
    public String unit;
    public BigDecimal rate;
    public BigDecimal cgstRate;
    public BigDecimal sgstRate;
    public boolean active = true;
}
