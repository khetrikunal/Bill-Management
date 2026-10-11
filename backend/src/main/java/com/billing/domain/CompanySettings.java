package com.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "company_settings")
public class CompanySettings {
    @Id public Long id = 1L;
    public String companyName;
    public String address;
    public String phone;
    public String email;
    public String gstin;
    public String bankName;
    public String bankAccount;
    public String bankBranch;
    public String ifsc;
    public String declaration;
    public String signatureText;
    public String footerNote;
    public String logo;
    public String defaultCurrency;
    public BigDecimal defaultCgstRate;
    public BigDecimal defaultSgstRate;
    public String pageSize;
    public String invoicePrefix;
    public int numberPadding;
    public boolean includeFinancialYear;
}
