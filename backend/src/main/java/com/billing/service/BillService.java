package com.billing.service;

import com.billing.domain.*;
import com.billing.dto.Dtos.*;
import com.billing.repo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
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

    public BillService(BillRepository bills, BillAuditRepository audits, SettingsRepository settingsRepo,
                       NumberingService numbering, CalculationService calc, ObjectMapper om) {
        this.bills = bills;
        this.audits = audits;
        this.settingsRepo = settingsRepo;
        this.numbering = numbering;
        this.calc = calc;
        this.om = om;
    }

    // ------------------------------------------------------------------ queries
    @Transactional(readOnly = true)
    public BillDto get(Long id) { return toDto(load(id)); }

    @Transactional(readOnly = true)
    public List<AuditDto> history(Long id) {
        return audits.findByBillIdOrderByCreatedAtAscIdAsc(id).stream()
                .map(a -> new AuditDto(a.action, a.username, a.detail, a.createdAt)).toList();
    }

    @Transactional(readOnly = true)
    public PageDto<BillSummary> search(String q, LocalDate from, LocalDate to, DocType type, BillStatus status,
                                       String sort, String dir, int page, int size) {
        String sortField = SORTABLE.contains(sort) ? sort : "createdAt";
        Sort.Direction d = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(d, sortField).and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Bill> res = bills.findAll(spec(q, from, to, type, status), p);
        return new PageDto<>(res.getContent().stream().map(this::toSummary).toList(), res.getNumber(),
                res.getSize(), res.getTotalElements(), res.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<BillDto> exportAll(String q, LocalDate from, LocalDate to, DocType type, BillStatus status) {
        Pageable p = PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "invoiceDate").and(Sort.by("id")));
        return bills.findAll(spec(q, from, to, type, status), p).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate first = LocalDate.now(zone).withDayOfMonth(1);
        Instant from = first.atStartOfDay(zone).toInstant();
        Instant to = first.plusMonths(1).atStartOfDay(zone).toInstant();
        List<CurrencyTotal> totals = bills.finalizedTotals(first, first.plusMonths(1)).stream()
                .map(r -> new CurrencyTotal((String) r[0], (BigDecimal) r[1])).toList();
        return new Dashboard(bills.count(), bills.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to),
                bills.countByStatus(BillStatus.DRAFT), bills.countByStatus(BillStatus.FINAL),
                bills.countByStatus(BillStatus.CANCELLED),
                bills.findTop5ByOrderByCreatedAtDesc().stream().map(this::toSummary).toList(), totals);
    }

    private Specification<Bill> spec(String q, LocalDate from, LocalDate to, DocType type, BillStatus status) {
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
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    // ------------------------------------------------------------------ commands
    @Transactional
    public BillDto create(BillRequest r, String user) {
        Bill b = new Bill();
        b.createdBy = user;
        b.status = BillStatus.DRAFT;
        apply(b, r);
        bills.save(b);
        audit(b.id, "CREATED", user, "Draft created (" + b.docType.label + ")");
        return toDto(b);
    }

    @Transactional
    public BillDto update(Long id, BillRequest r, String user) {
        Bill b = load(id);
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
        Bill b = load(id);
        if (b.status != BillStatus.DRAFT) throw ApiException.conflict("Only a draft can be finalized.");
        if (b.items.isEmpty() || b.grandTotal.signum() <= 0) {
            throw ApiException.badRequest("A bill needs at least one item with a positive total.");
        }
        calc.calculate(b); // re-verify totals from line items before locking them in
        CompanySettings s = settingsRepo.findById(1L).orElseThrow();
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
        Bill b = load(id);
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
        Bill b = load(id);
        if (b.status != BillStatus.DRAFT) throw ApiException.conflict("Only drafts can be deleted. Cancel finalized bills instead.");
        bills.delete(b);
        audit(id, "DRAFT_DELETED", user, "Draft deleted");
    }

    @Transactional
    public BillDto duplicate(Long id, String user) {
        Bill src = load(id);
        Bill n = new Bill();
        n.docType = src.docType;
        n.status = BillStatus.DRAFT;
        n.invoiceDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        n.transactionRef = src.transactionRef;
        n.poNumber = src.poNumber;
        n.buyerId = src.buyerId;
        n.buyerName = src.buyerName;
        n.buyerAddress = src.buyerAddress;
        n.buyerGstin = src.buyerGstin;
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

    // ------------------------------------------------------------------ helpers
    private Bill load(Long id) {
        return bills.findById(id).orElseThrow(() -> ApiException.notFound("Bill not found"));
    }

    private void apply(Bill b, BillRequest r) {
        b.docType = r.docType();
        b.invoiceDate = r.invoiceDate();
        b.transactionRef = blankToNull(r.transactionRef());
        b.buyerId = r.buyerId();
        b.buyerName = r.buyerName().trim();
        b.buyerAddress = r.buyerAddress().trim();
        b.buyerGstin = r.buyerGstin() == null ? "" : r.buyerGstin().trim();
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
        return new BillSummary(b.id, b.docType, b.docType.label, b.status, b.invoiceNumber, display(b), b.buyerName,
                b.invoiceDate, b.currency, b.grandTotal, b.createdAt);
    }

    BillDto toDto(Bill b) {
        SellerSnapshot seller;
        try {
            seller = b.sellerSnapshot != null ? om.readValue(b.sellerSnapshot, SellerSnapshot.class)
                    : SellerSnapshot.of(settingsRepo.findById(1L).orElseThrow());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt seller snapshot for bill " + b.id, e);
        }
        List<ItemDto> items = b.items.stream().map(i -> new ItemDto(i.id, i.poNumber, i.name, i.itemCode, i.hsn,
                i.qty, i.unit, i.rate, i.cgstRate, i.sgstRate, i.taxable, i.cgst, i.sgst, i.total)).toList();
        return new BillDto(b.id, b.docType, b.docType.label, b.status, b.invoiceNumber, display(b), b.invoiceDate,
                b.transactionRef, b.poNumber, b.buyerId, b.buyerName, b.buyerAddress, b.buyerGstin, b.currency,
                b.notes, seller, items, b.totalTaxable, b.totalCgst, b.totalSgst, b.totalTax, b.grandTotal,
                b.amountInWords, b.createdBy, b.createdAt, b.updatedAt, b.finalizedAt, b.cancelledAt,
                b.cancelReason, b.duplicatedFrom, b.version);
    }
}
