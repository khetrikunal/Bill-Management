package com.billing.dto;

import com.billing.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() { }

    public static final String GSTIN_RE = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";

    // ---------- auth / users ----------
    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record UserDto(Long id, String username, String fullName, Role role, boolean active) {
        public static UserDto of(AppUser u) { return new UserDto(u.id, u.username, u.fullName, u.role, u.active); }
    }
    public record LoginResponse(String token, UserDto user) { }
    public record UserCreate(@NotBlank @Size(min = 3, max = 60) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
                             @NotBlank @Size(max = 120) String fullName,
                             @NotNull Role role,
                             @NotBlank @Size(min = 8, max = 100) String password) { }
    public record UserUpdate(@Size(max = 120) String fullName, Role role, Boolean active,
                             @Size(min = 8, max = 100) String password) { }
    public record CompanyAccessRequest(@NotNull List<@NotNull Long> companyIds) { }
    public record PasswordChange(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) { }

    // ---------- settings ----------
    public record SettingsDto(
            Long id,
            @NotBlank @Size(max = 200) String companyName,
            @Size(max = 200) String displayName,
            @Size(max = 1000) String address,
            @Size(max = 1000) String billingAddress,
            @Size(max = 200) String phone,
            @Size(max = 200) String email,
            @Pattern(regexp = GSTIN_RE) String gstin,
            @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$") String pan,
            @Size(max = 120) String bankName,
            @Size(max = 60) String bankAccount,
            @Size(max = 120) String bankBranch,
            @Size(max = 20) String ifsc,
            @Size(max = 120) String upiId,
            @Size(max = 500) String defaultPaymentTerms,
            @Size(max = 1000) String declaration,
            @Size(max = 300) String signatureText,
            @Size(max = 200) String authorizedSignatory,
            @Size(max = 120) String signatoryDesignation,
            @Size(max = 1000) String footerNote,
            @Size(max = 600000) String logo,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String defaultCurrency,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal defaultCgstRate,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal defaultSgstRate,
            @NotBlank @Pattern(regexp = "A4|A5|LETTER|LEGAL") String pageSize,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,20}") String invoicePrefix,
            @Min(1) @Max(8) int numberPadding,
            boolean includeFinancialYear) {
        public static SettingsDto of(CompanySettings s) {
            return new SettingsDto(s.id, s.companyName, s.displayName, s.address, s.billingAddress, s.phone, s.email,
                    s.gstin, s.pan, s.bankName, s.bankAccount, s.bankBranch, s.ifsc, s.upiId,
                    s.defaultPaymentTerms, s.declaration, s.signatureText, s.authorizedSignatory,
                    s.signatoryDesignation, s.footerNote, s.logo, s.defaultCurrency, s.defaultCgstRate,
                    s.defaultSgstRate, s.pageSize, s.invoicePrefix, s.numberPadding, s.includeFinancialYear);
        }
    }

    /** Seller details frozen into a bill at finalization so later settings changes never alter old invoices. */
    public record SellerSnapshot(String companyName, String displayName, String address, String billingAddress,
                                 String phone, String email, String gstin, String pan,
                                 String bankName, String bankAccount, String bankBranch, String ifsc,
                                 String upiId, String defaultPaymentTerms, String declaration, String signatureText,
                                 String authorizedSignatory, String signatoryDesignation, String footerNote,
                                 String logo, String pageSize) {
        public static SellerSnapshot of(CompanySettings s) {
            return new SellerSnapshot(s.companyName, s.displayName, s.address, s.billingAddress, s.phone, s.email,
                    s.gstin, s.pan, s.bankName, s.bankAccount, s.bankBranch, s.ifsc, s.upiId,
                    s.defaultPaymentTerms, s.declaration, s.signatureText, s.authorizedSignatory,
                    s.signatoryDesignation, s.footerNote, s.logo, s.pageSize);
        }
    }

    public record CompanyOption(Long id, String displayName) { }

    // ---------- master data ----------
    public record CustomerDto(Long id, @NotBlank @Size(max = 200) String name, @Size(max = 1000) String address,
                              @Pattern(regexp = GSTIN_RE) String gstin, @Size(max = 200) String email,
                              @Size(max = 60) String phone) {
        public static CustomerDto of(Customer c) { return new CustomerDto(c.id, c.name, c.address, c.gstin, c.email, c.phone); }
    }
    public record ProductDto(Long id, @NotBlank @Size(max = 300) String name, @Size(max = 80) String itemCode,
                             @Pattern(regexp = "^$|^\\d{4,8}$") String hsn, @NotBlank @Size(max = 20) String unit,
                             @NotNull @DecimalMin("0") BigDecimal rate,
                             @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal cgstRate,
                             @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal sgstRate, boolean active) {
        public static ProductDto of(Product p) {
            return new ProductDto(p.id, p.name, p.itemCode, p.hsn, p.unit, p.rate, p.cgstRate, p.sgstRate, p.active);
        }
    }
    public record ImportRow(int row, String name, String itemCode, String hsn, String unit, BigDecimal rate,
                            BigDecimal cgstRate, BigDecimal sgstRate, String status, String message) { }
    public record ImportResult(List<ImportRow> rows, int newCount, int duplicateCount, int errorCount,
                               boolean committed, int created) { }

    // ---------- bills ----------
    public record ItemDto(Long id,
                          @Size(max = 60) String poNumber,
                          @NotBlank @Size(max = 300) String name,
                          @Size(max = 80) String itemCode,
                          @Pattern(regexp = "^$|^\\d{4,8}$", message = "HSN/SAC must be 4 to 8 digits") String hsn,
                          @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3) BigDecimal qty,
                          @NotBlank @Size(max = 20) String unit,
                          @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal rate,
                          @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal cgstRate,
                          @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal sgstRate,
                          BigDecimal taxable, BigDecimal cgst, BigDecimal sgst, BigDecimal total) { }

    public record BillRequest(@NotNull DocType docType,
                              @NotNull LocalDate invoiceDate,
                              @Size(max = 60) String transactionRef,
                              @Size(max = 60) String poNumber,
                              Long buyerId,
                              @NotBlank @Size(max = 200) String buyerName,
                              @NotBlank @Size(max = 1000) String buyerAddress,
                              @Pattern(regexp = GSTIN_RE, message = "Invalid GSTIN format") String buyerGstin,
                              @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
                              @Size(max = 2000) String notes,
                              Long version,
                              @NotEmpty @Size(max = 200) List<@Valid ItemDto> items,
                              LocalDate dueDate,
                              @Size(max = 500) String paymentTerms,
                              @Size(max = 60) String buyerPhone,
                              @Email @Size(max = 200) String buyerEmail,
                              @Size(max = 1000) String shippingAddress) {
        public BillRequest(DocType docType, LocalDate invoiceDate, String transactionRef, String poNumber,
                           Long buyerId, String buyerName, String buyerAddress, String buyerGstin, String currency,
                           String notes, Long version, List<ItemDto> items) {
            this(docType, invoiceDate, transactionRef, poNumber, buyerId, buyerName, buyerAddress, buyerGstin,
                    currency, notes, version, items, null, "", "", "", "");
        }
    }

    public record CancelRequest(@NotBlank @Size(min = 3, max = 500) String reason) { }

    public record BillDto(Long id, Long companyId, String companyDisplayName, DocType docType, String docTypeLabel,
                          BillStatus status, String invoiceNumber,
                          String displayNumber, LocalDate invoiceDate, LocalDate dueDate, String paymentTerms,
                          String transactionRef, String poNumber, Long buyerId, String buyerName, String buyerAddress,
                          String buyerGstin, String buyerPhone, String buyerEmail, String shippingAddress, String currency,
                          String notes, SellerSnapshot seller, List<ItemDto> items, BigDecimal totalTaxable,
                          BigDecimal totalCgst, BigDecimal totalSgst, BigDecimal totalTax, BigDecimal grandTotal,
                          String amountInWords, String createdBy, Instant createdAt, Instant updatedAt,
                          Instant finalizedAt, Instant cancelledAt, String cancelReason, Long duplicatedFrom,
                          Long version, BigDecimal amountPaid, BigDecimal outstandingBalance, String paymentStatus,
                          List<PaymentDto> payments) { }

    public record BillSummary(Long id, Long companyId, String companyDisplayName, DocType docType, String docTypeLabel,
                              BillStatus status, String invoiceNumber,
                              String displayNumber, String buyerName, LocalDate invoiceDate, LocalDate dueDate, String currency,
                              BigDecimal grandTotal, BigDecimal amountPaid, BigDecimal outstandingBalance,
                              String paymentStatus, Instant createdAt) { }

    public record PaymentRequest(@NotNull @DecimalMin(value = "0", inclusive = false)
                                 @Digits(integer = 14, fraction = 2) BigDecimal amount,
                                 @NotNull LocalDate paymentDate,
                                 @NotBlank @Pattern(regexp = "CASH|BANK_TRANSFER|UPI|CHEQUE|OTHER") String method,
                                 @Size(max = 120) String transactionReference,
                                 @Size(max = 1000) String notes) { }
    public record PaymentDto(Long id, BigDecimal amount, LocalDate paymentDate, String method,
                             String transactionReference, String notes, String recordedBy, Instant createdAt) { }

    public record AuditDto(String action, String username, String detail, Instant createdAt) { }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) { }

    public record DocTypeDto(String code, String label, String description) { }

    public record Dashboard(long total, long thisMonth, long drafts, long finalized, long cancelled,
                            List<BillSummary> recent, List<CurrencyTotal> finalizedThisMonth,
                            List<CompanyDashboard> companies, long overdue) { }
    public record CurrencyTotal(String currency, BigDecimal total) { }
    public record CompanyDashboard(Long companyId, String companyDisplayName, long invoiceCount,
                                   List<CurrencyLedger> totals) { }
    public record CurrencyLedger(String currency, BigDecimal billed, BigDecimal received,
                                 BigDecimal outstanding) { }
}
