package com.billing.service;

import com.billing.domain.*;
import com.billing.dto.Dtos.*;
import com.billing.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class BillService {
    private static final Set<String> SORTABLE = Set.of("invoiceDate", "grandTotal", "createdAt", "invoiceNumber", "buyerName");

    private final BillRepository bills;
    private final BillAuditRepository audits;
    private final SettingsRepository settingsRepo;
    private final NumberingService numbering;
    private final CalculationService calc;
    private final ObjectMapper om;
    private final BillPaymentRepository payments;

    public BillService(BillRepository bills, BillAuditRepository audits, SettingsRepository settingsRepo,
                       NumberingService numbering, CalculationService calc, ObjectMapper om) {
        this(bills, audits, settingsRepo, numbering, calc, om, null);
    }

    @Autowired
    public BillService(BillRepository bills, BillAuditRepository audits, SettingsRepository settingsRepo,
                       NumberingService numbering, CalculationService calc, ObjectMapper om,
                       BillPaymentRepository payments) {
        this.bills = bills;
        this.audits = audits;
        this.settingsRepo = settingsRepo;
        this.numbering = numbering;
        this.calc = calc;
        this.om = om;
        this.payments = payments;
    }

    // ------------------------------------------------------------------ queries
    @Transactional(readOnly = true)
    public BillDto get(Long id) { return get(id, 1L); }

    @Transactional(readOnly = true)
    public BillDto get(Long id, Long companyId) { return toDto(load(id, companyId)); }

    @Transactional(readOnly = true)
    public List<AuditDto> history(Long id) { return history(id, 1L); }

    @Transactional(readOnly = true)
    public List<AuditDto> history(Long id, Long companyId) {
        load(id, companyId);
        return audits.findByBillIdOrderByCreatedAtAscIdAsc(id).stream()
                .map(a -> new AuditDto(a.action, a.username, a.detail, a.createdAt)).toList();
    }

    @Transactional(readOnly = true)
    public PageDto<BillSummary> search(String q, LocalDate from, LocalDate to, DocType type, BillStatus status,
                                       String sort, String dir, int page, int size) {
        return search(q, from, to, type, status, null, sort, dir, page, size, 1L);
    }

    @Transactional(readOnly = true)
    public PageDto<BillSummary> search(String q, LocalDate from, LocalDate to, DocType type, BillStatus status,
                                       String paymentStatus, String sort, String dir, int page, int size, Long companyId) {
        String sortField = SORTABLE.contains(sort) ? sort : "createdAt";
        Sort.Direction d = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(d, sortField).and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Bill> res = bills.findAll(spec(q, from, to, type, status, paymentStatus, companyId), p);
        return new PageDto<>(res.getContent().stream().map(this::toSummary).toList(), res.getNumber(),
                res.getSize(), res.getTotalElements(), res.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<BillDto> exportAll(String q, LocalDate from, LocalDate to, DocType type, BillStatus status) {
        return exportAll(q, from, to, type, status, null, 1L);
    }

    @Transactional(readOnly = true)
    public List<BillDto> exportAll(String q, LocalDate from, LocalDate to, DocType type, BillStatus status,
                                   String paymentStatus, Long companyId) {
        Pageable p = PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "invoiceDate").and(Sort.by("id")));
        return bills.findAll(spec(q, from, to, type, status, paymentStatus, companyId), p).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard() { return dashboard(1L); }

    @Transactional(readOnly = true)
    public Dashboard dashboard(Long companyId) {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate first = LocalDate.now(zone).withDayOfMonth(1);
        Instant from = first.atStartOfDay(zone).toInstant();
        Instant to = first.plusMonths(1).atStartOfDay(zone).toInstant();
        List<Object[]> rawTotals = companyId == null ? bills.finalizedTotals(first, first.plusMonths(1))
                : bills.finalizedTotalsByCompany(companyId, first, first.plusMonths(1));
        List<CurrencyTotal> totals = rawTotals.stream()
                .map(r -> new CurrencyTotal((String) r[0], (BigDecimal) r[1])).toList();
        long count = companyId == null ? bills.count() : bills.countByCompanyId(companyId);
        long monthCount = companyId == null ? bills.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)
                : bills.countByCompanyIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(companyId, from, to);
        long drafts = countStatus(companyId, BillStatus.DRAFT);
        long finalized = countStatus(companyId, BillStatus.FINAL);
        long cancelled = countStatus(companyId, BillStatus.CANCELLED);
        LocalDate today = LocalDate.now(zone);
        long overdue = companyId == null ? bills.countOverdue(today) : bills.countOverdueByCompany(companyId, today);
        List<Bill> recent = companyId == null ? bills.findTop5ByOrderByCreatedAtDesc()
                : bills.findTop5ByCompanyIdOrderByCreatedAtDesc(companyId);
        List<CompanyDashboard> companyTotals = settingsRepo.findAll().stream()
                .filter(company -> companyId == null || Objects.equals(company.id, companyId))
                .map(company -> companyDashboard(company.id, company)).toList();
        return new Dashboard(count, monthCount, drafts, finalized, cancelled,
                recent.stream().map(this::toSummary).toList(), totals, companyTotals, overdue);
    }

    private CompanyDashboard companyDashboard(Long id, CompanySettings company) {
        Map<String, BigDecimal> billed = totalsMap(bills.billedTotalsByCompany(id));
        Map<String, BigDecimal> received = totalsMap(payments.receivedTotalsByCompany(id));
        Set<String> currencies = new TreeSet<>(billed.keySet());
        currencies.addAll(received.keySet());
        List<CurrencyLedger> ledger = currencies.stream().map(currency -> {
            BigDecimal gross = billed.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal paid = received.getOrDefault(currency, BigDecimal.ZERO);
            return new CurrencyLedger(currency, gross, paid, gross.subtract(paid).max(BigDecimal.ZERO));
        }).toList();
        return new CompanyDashboard(id, companyDisplayName(company),
                bills.countByCompanyIdAndStatus(id, BillStatus.FINAL), ledger);
    }

    private static Map<String, BigDecimal> totalsMap(List<Object[]> rows) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Object[] row : rows) result.put((String) row[0], (BigDecimal) row[1]);
        return result;
    }

    private long countStatus(Long companyId, BillStatus status) {
        return companyId == null ? bills.countByStatus(status) : bills.countByCompanyIdAndStatus(companyId, status);
    }

    private Specification<Bill> spec(String q, LocalDate from, LocalDate to, DocType type, BillStatus status,
                                     String paymentStatus, Long companyId) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("invoiceNumber")), like, '\\'),
                        cb.like(cb.lower(root.get("buyerName")), like, '\\'),
                        cb.like(cb.lower(root.get("poNumber")), like, '\\'),
                        cb.like(cb.lower(root.get("transactionRef")), like, '\\')));
            }
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), to));
            if (type != null) ps.add(cb.equal(root.get("docType"), type));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (companyId != null) ps.add(cb.equal(root.get("companyId"), companyId));
            if (paymentStatus != null && !paymentStatus.isBlank()) {
                String normalized = paymentStatus.toUpperCase(Locale.ROOT);
                Subquery<BigDecimal> paidSubquery = query.subquery(BigDecimal.class);
                var payment = paidSubquery.from(BillPayment.class);
                paidSubquery.select(cb.sum(payment.get("amount")));
                paidSubquery.where(cb.equal(payment.get("bill").get("id"), root.get("id")));
                var paid = cb.coalesce(paidSubquery, BigDecimal.ZERO);
                var total = root.<BigDecimal>get("grandTotal");
                if ("UNPAID".equals(normalized)) ps.add(cb.and(cb.equal(root.get("status"), BillStatus.FINAL),
                        cb.equal(paid, BigDecimal.ZERO)));
                else if ("PARTIALLY_PAID".equals(normalized)) {
                    ps.add(cb.and(cb.equal(root.get("status"), BillStatus.FINAL),
                            cb.greaterThan(paid, BigDecimal.ZERO), cb.lessThan(paid, total)));
                } else if ("PAID".equals(normalized)) ps.add(cb.and(cb.equal(root.get("status"), BillStatus.FINAL),
                        cb.greaterThanOrEqualTo(paid, total)));
                else if ("OVERDUE".equals(normalized)) {
                    ps.add(cb.and(cb.equal(root.get("status"), BillStatus.FINAL),
                            cb.lessThan(root.<LocalDate>get("dueDate"), LocalDate.now(ZoneId.of("Asia/Kolkata"))),
                            cb.lessThan(paid, total)));
                } else if ("CANCELLED".equals(normalized)) ps.add(cb.equal(root.get("status"), BillStatus.CANCELLED));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    // ------------------------------------------------------------------ commands
    @Transactional
    public BillDto create(BillRequest r, String user) {
        return create(r, user, 1L);
    }

    @Transactional
    public BillDto create(BillRequest r, String user, Long companyId) {
        requireCompany(companyId);
        Bill b = new Bill();
        b.companyId = companyId;
        b.createdBy = user;
        b.status = BillStatus.DRAFT;
        apply(b, r);
        bills.save(b);
        audit(b.id, "CREATED", user, "Draft created (" + b.docType.label + ")");
        return toDto(b);
    }

    @Transactional
    public BillDto update(Long id, BillRequest r, String user) {
        return update(id, r, user, 1L);
    }

    @Transactional
    public BillDto update(Long id, BillRequest r, String user, Long companyId) {
        Bill b = load(id, companyId);
        if (b.status != BillStatus.DRAFT) {
            throw ApiException.conflict("Only draft bills can be edited. Duplicate this bill to make a corrected one.");
        }
        if (r.version() != null && !r.version().equals(b.version)) {
            throw ApiException.conflict("This draft was changed by someone else. Reload and try again.");
        }
        if (r.docType() != b.docType) {
            throw ApiException.badRequest("The document type of an existing bill cannot be changed.");
        }
        apply(b, r);
        b.updatedAt = Instant.now();
        bills.save(b);
        audit(b.id, "UPDATED", user, "Draft edited");
        return toDto(b);
    }

    @Transactional
    public BillDto finalizeBill(Long id, String user) {
        return finalizeBill(id, user, 1L);
    }

    @Transactional
    public BillDto finalizeBill(Long id, String user, Long companyId) {
        Bill b = load(id, companyId);
        if (b.status != BillStatus.DRAFT) throw ApiException.conflict("Only a draft can be finalized.");
        if (b.items.isEmpty() || b.grandTotal.signum() <= 0) {
            throw ApiException.badRequest("A bill needs at least one item with a positive total.");
        }
        calc.calculate(b); // re-verify totals from line items before locking them in
        CompanySettings s = settingsRepo.findById(companyId).orElseThrow();
        if (s.companyName == null || s.companyName.isBlank()) {
            throw ApiException.badRequest("Fill in Company Settings before finalizing bills.");
        }
        b.invoiceNumber = numbering.next(s, b.invoiceDate);
        try {
            b.sellerSnapshot = om.writeValueAsString(SellerSnapshot.of(s));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        b.status = BillStatus.FINAL;
        b.finalizedAt = Instant.now();
        b.updatedAt = b.finalizedAt;
        bills.save(b);
        audit(b.id, "FINALIZED", user, "Invoice number " + b.invoiceNumber + " assigned");
        return toDto(b);
    }

    @Transactional
    public BillDto cancel(Long id, String reason, String user) {
        return cancel(id, reason, user, 1L);
    }

    @Transactional
    public BillDto cancel(Long id, String reason, String user, Long companyId) {
        Bill b = load(id, companyId);
        if (b.status != BillStatus.FINAL) throw ApiException.conflict("Only finalized bills can be cancelled.");
        b.status = BillStatus.CANCELLED;
        b.cancelledAt = Instant.now();
        b.cancelReason = reason.trim();
        b.updatedAt = b.cancelledAt;
        bills.save(b);
        audit(b.id, "CANCELLED", user, reason.trim());
        return toDto(b);
    }

    @Transactional
    public void deleteDraft(Long id, String user) {
        deleteDraft(id, user, 1L);
    }

    @Transactional
    public void deleteDraft(Long id, String user, Long companyId) {
        Bill b = load(id, companyId);
        if (b.status != BillStatus.DRAFT) throw ApiException.conflict("Only drafts can be deleted. Cancel finalized bills instead.");
        bills.delete(b);
        audit(id, "DRAFT_DELETED", user, "Draft deleted");
    }

    @Transactional
    public BillDto duplicate(Long id, String user) {
        return duplicate(id, user, 1L);
    }

    @Transactional
    public BillDto duplicate(Long id, String user, Long companyId) {
        Bill src = load(id, companyId);
        Bill n = new Bill();
        n.companyId = src.companyId;
        n.docType = src.docType;
        n.status = BillStatus.DRAFT;
        n.invoiceDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        n.paymentTerms = src.paymentTerms;
        n.transactionRef = src.transactionRef;
        n.poNumber = src.poNumber;
        n.buyerId = src.buyerId;
        n.buyerName = src.buyerName;
        n.buyerAddress = src.buyerAddress;
        n.buyerGstin = src.buyerGstin;
        n.buyerPhone = src.buyerPhone;
        n.buyerEmail = src.buyerEmail;
        n.shippingAddress = src.shippingAddress;
        n.currency = src.currency;
        n.notes = src.notes;
        n.createdBy = user;
        n.duplicatedFrom = src.id;
        for (BillItem si : src.items) {
            BillItem i = new BillItem();
            i.bill = n;
            i.lineNo = si.lineNo;
            i.poNumber = si.poNumber;
            i.name = si.name;
            i.itemCode = si.itemCode;
            i.hsn = si.hsn;
            i.qty = si.qty;
            i.unit = si.unit;
            i.rate = si.rate;
            i.cgstRate = si.cgstRate;
            i.sgstRate = si.sgstRate;
            n.items.add(i);
        }
        calc.calculate(n);
        bills.save(n);
        audit(n.id, "CREATED", user, "Duplicated from " + display(src));
        audit(src.id, "DUPLICATED", user, "Copy created as draft #" + n.id);
        return toDto(n);
    }

    @Transactional
    public BillDto recordPayment(Long id, PaymentRequest request, String user, Long companyId) {
        Bill bill = load(id, companyId);
        if (bill.status != BillStatus.FINAL) {
            throw ApiException.conflict("Payments can only be recorded against finalized invoices.");
        }
        BigDecimal paid = totalPaid(bill);
        BigDecimal remaining = bill.grandTotal.subtract(paid);
        BigDecimal amount = CalculationService.r2(request.amount());
        if (amount.compareTo(remaining) > 0) {
            throw ApiException.badRequest("Payment exceeds the outstanding balance.");
        }
        BillPayment payment = new BillPayment();
        payment.bill = bill;
        payment.amount = amount;
        payment.paymentDate = request.paymentDate();
        payment.method = request.method();
        payment.transactionReference = request.transactionReference() == null ? "" : request.transactionReference().trim();
        payment.notes = request.notes() == null ? "" : request.notes().trim();
        payment.recordedBy = user;
        payments.save(payment);
        bill.payments.add(payment);
        bill.updatedAt = Instant.now();
        bills.save(bill);
        audit(bill.id, "PAYMENT_RECORDED", user,
                amount.toPlainString() + " " + bill.currency + " via " + payment.method);
        return toDto(bill);
    }

    // ------------------------------------------------------------------ helpers
    private Bill load(Long id) {
        return bills.findById(id).orElseThrow(() -> ApiException.notFound("Bill not found"));
    }

    private Bill load(Long id, Long companyId) {
        Bill bill = load(id);
        if (!Objects.equals(bill.companyId, companyId)) throw ApiException.notFound("Bill not found");
        return bill;
    }

    private CompanySettings requireCompany(Long companyId) {
        if (companyId == null) throw ApiException.badRequest("A company must be selected.");
        return settingsRepo.findById(companyId)
                .orElseThrow(() -> ApiException.notFound("Company not found"));
    }

    private void apply(Bill b, BillRequest r) {
        b.docType = r.docType();
        b.invoiceDate = r.invoiceDate();
        b.dueDate = r.dueDate();
        b.paymentTerms = r.paymentTerms() == null ? "" : r.paymentTerms().trim();
        b.transactionRef = blankToNull(r.transactionRef());
        b.buyerId = r.buyerId();
        b.buyerName = r.buyerName().trim();
        b.buyerAddress = r.buyerAddress().trim();
        b.buyerGstin = r.buyerGstin() == null ? "" : r.buyerGstin().trim();
        b.buyerPhone = r.buyerPhone() == null ? "" : r.buyerPhone().trim();
        b.buyerEmail = r.buyerEmail() == null ? "" : r.buyerEmail().trim();
        b.shippingAddress = r.shippingAddress() == null ? "" : r.shippingAddress().trim();
        b.currency = r.currency();
        b.notes = blankToNull(r.notes());

        List<ItemDto> items = new ArrayList<>(r.items());
        if (b.docType == DocType.TAX_INVOICE) {
            if (isBlank(r.poNumber())) throw ApiException.badRequest("P.O. number is required for a single-P.O. tax invoice.");
            b.poNumber = r.poNumber().trim();
        } else {
            b.poNumber = null; // multi-P.O. documents keep P.O. numbers per item, never in the header
            for (ItemDto i : items) {
                if (isBlank(i.poNumber())) throw ApiException.badRequest("Every item needs a P.O. number in a multi-P.O. invoice.");
            }
            // keep items of the same P.O. together, in first-appearance order
            List<String> order = new ArrayList<>();
            for (ItemDto i : items) if (!order.contains(i.poNumber().trim())) order.add(i.poNumber().trim());
            items.sort(Comparator.comparingInt(i -> order.indexOf(i.poNumber().trim())));
        }

        b.items.clear();
        int n = 1;
        for (ItemDto d : items) {
            BillItem i = new BillItem();
            i.bill = b;
            i.lineNo = n++;
            i.poNumber = b.docType == DocType.TAX_INVOICE ? null : d.poNumber().trim();
            i.name = d.name().trim();
            i.itemCode = blankToNull(d.itemCode());
            i.hsn = blankToNull(d.hsn());
            i.qty = d.qty();
            i.unit = d.unit().trim();
            i.rate = d.rate();
            i.cgstRate = d.cgstRate();
            i.sgstRate = d.sgstRate();
            b.items.add(i);
        }
        calc.calculate(b);
    }

    private void audit(Long billId, String action, String user, String detail) {
        BillAudit a = new BillAudit();
        a.billId = billId;
        a.action = action;
        a.username = user;
        a.detail = detail;
        audits.save(a);
    }

    static String display(Bill b) { return b.invoiceNumber != null ? b.invoiceNumber : "Draft #" + b.id; }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String blankToNull(String s) { return isBlank(s) ? null : s.trim(); }

    BillSummary toSummary(Bill b) {
        BigDecimal paid = totalPaid(b);
        CompanySettings company = requireCompany(b.companyId);
        return new BillSummary(b.id, b.companyId, companyDisplayName(company), b.docType, b.docType.label, b.status,
                b.invoiceNumber, display(b), b.buyerName, b.invoiceDate, b.dueDate, b.currency, b.grandTotal, paid,
                b.grandTotal.subtract(paid).max(BigDecimal.ZERO), paymentStatus(b, paid), b.createdAt);
    }

    BillDto toDto(Bill b) {
        SellerSnapshot seller;
        try {
            seller = b.sellerSnapshot != null ? om.readValue(b.sellerSnapshot, SellerSnapshot.class)
                    : SellerSnapshot.of(requireCompany(b.companyId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt seller snapshot for bill " + b.id, e);
        }
        CompanySettings company = requireCompany(b.companyId);
        List<ItemDto> items = b.items.stream().map(i -> new ItemDto(i.id, i.poNumber, i.name, i.itemCode, i.hsn,
                i.qty, i.unit, i.rate, i.cgstRate, i.sgstRate, i.taxable, i.cgst, i.sgst, i.total)).toList();
        BigDecimal paid = totalPaid(b);
        List<PaymentDto> paymentDtos = b.payments.stream().map(p -> new PaymentDto(p.id, p.amount, p.paymentDate,
                p.method, p.transactionReference, p.notes, p.recordedBy, p.createdAt)).toList();
        return new BillDto(b.id, b.companyId, companyDisplayName(company), b.docType, b.docType.label, b.status,
                b.invoiceNumber, display(b), b.invoiceDate, b.dueDate, b.paymentTerms,
                b.transactionRef, b.poNumber, b.buyerId, b.buyerName, b.buyerAddress, b.buyerGstin,
                b.buyerPhone, b.buyerEmail, b.shippingAddress, b.currency,
                b.notes, seller, items, b.totalTaxable, b.totalCgst, b.totalSgst, b.totalTax, b.grandTotal,
                b.amountInWords, b.createdBy, b.createdAt, b.updatedAt, b.finalizedAt, b.cancelledAt,
                b.cancelReason, b.duplicatedFrom, b.version, paid, b.grandTotal.subtract(paid).max(BigDecimal.ZERO),
                paymentStatus(b, paid), paymentDtos);
    }

    private static BigDecimal totalPaid(Bill bill) {
        return bill.payments.stream().map(p -> p.amount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String paymentStatus(Bill bill, BigDecimal paid) {
        if (bill.status == BillStatus.CANCELLED) return "CANCELLED";
        if (bill.status == BillStatus.FINAL && bill.dueDate != null
                && bill.dueDate.isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")))
                && paid.compareTo(bill.grandTotal) < 0) return "OVERDUE";
        if (paid.signum() == 0) return "UNPAID";
        return paid.compareTo(bill.grandTotal) >= 0 ? "PAID" : "PARTIALLY_PAID";
    }

    private static String companyDisplayName(CompanySettings settings) {
        return settings.displayName == null || settings.displayName.isBlank()
                ? settings.companyName : settings.displayName;
    }
}
