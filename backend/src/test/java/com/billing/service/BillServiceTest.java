package com.billing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.billing.domain.*;
import com.billing.dto.Dtos.*;
import com.billing.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BillServiceTest {
    private BillRepository bills;
    private BillAuditRepository audits;
    private SettingsRepository settingsRepo;
    private NumberingService numbering;
<<<<<<< HEAD
    private BillPaymentRepository payments;
=======
>>>>>>> origin/main
    private BillService svc;
    private CompanySettings settings;

    @BeforeEach
    void setUp() {
        bills = mock(BillRepository.class);
        audits = mock(BillAuditRepository.class);
        settingsRepo = mock(SettingsRepository.class);
        numbering = mock(NumberingService.class);
<<<<<<< HEAD
        payments = mock(BillPaymentRepository.class);
=======
>>>>>>> origin/main
        settings = new CompanySettings();
        settings.companyName = "Test Co";
        settings.invoicePrefix = "T";
        settings.pageSize = "A4";
        when(settingsRepo.findById(1L)).thenReturn(Optional.of(settings));
<<<<<<< HEAD
        CompanySettings second = new CompanySettings();
        second.id = 2L;
        second.companyName = "PK Corporate Fabric Manufacturing";
        second.displayName = second.companyName;
        when(settingsRepo.findById(2L)).thenReturn(Optional.of(second));
=======
>>>>>>> origin/main
        when(bills.save(any(Bill.class))).thenAnswer(inv -> {
            Bill b = inv.getArgument(0);
            if (b.id == null) b.id = 1L;
            return b;
        });
<<<<<<< HEAD
        svc = new BillService(bills, audits, settingsRepo, numbering, new CalculationService(), new ObjectMapper(), payments);
=======
        svc = new BillService(bills, audits, settingsRepo, numbering, new CalculationService(), new ObjectMapper());
>>>>>>> origin/main
    }

    private static ItemDto item(String po) {
        return new ItemDto(null, po, "Nirma", "O07201052847", "3402", new BigDecimal("30"), "kg",
                new BigDecimal("40"), new BigDecimal("9"), new BigDecimal("9"), null, null, null, null);
    }

    private static BillRequest request(DocType t, String header, ItemDto... items) {
        return new BillRequest(t, LocalDate.of(2026, 4, 28), null, header, null, "KALYANI TECHNOFORGE LTD.",
                "Plot A-7, MIDC Baramati", "", "INR", null, null, List.of(items));
    }

    @Test
    void createsDraftWithCalculatedTotals() {
        BillDto d = svc.create(request(DocType.TAX_INVOICE, "6100015187", item(null)), "ravi");
        assertEquals(BillStatus.DRAFT, d.status());
        assertNull(d.invoiceNumber());
        assertEquals(0, new BigDecimal("1416.00").compareTo(d.grandTotal()));
        verify(numbering, never()).next(any(), any());
    }

    @Test
<<<<<<< HEAD
    void companySelectionIsStoredAndCannotReadAnotherCompanysBill() {
        BillDto draft = svc.create(request(DocType.TAX_INVOICE, "1", item(null)), "ravi", 2L);
        assertEquals(2L, draft.companyId());
        assertEquals("PK Corporate Fabric Manufacturing", draft.companyDisplayName());

        Bill stored = new Bill();
        stored.id = draft.id();
        stored.companyId = 2L;
        stored.docType = DocType.TAX_INVOICE;
        stored.status = BillStatus.DRAFT;
        when(bills.findById(draft.id())).thenReturn(Optional.of(stored));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> svc.get(draft.id(), 1L)).status);
    }

    @Test
    void paymentEntryUpdatesOutstandingAndIsAudited() {
        Bill bill = new Bill();
        bill.id = 8L;
        bill.companyId = 1L;
        bill.status = BillStatus.FINAL;
        bill.docType = DocType.TAX_INVOICE;
        bill.invoiceDate = LocalDate.of(2026, 4, 28);
        bill.currency = "INR";
        bill.buyerName = "Buyer";
        bill.buyerAddress = "Address";
        bill.grandTotal = new BigDecimal("100.00");
        when(bills.findById(8L)).thenReturn(Optional.of(bill));
        when(payments.save(any(BillPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillDto result = svc.recordPayment(8L,
                new PaymentRequest(new BigDecimal("35"), LocalDate.of(2026, 5, 1), "UPI", "TXN-1", "Advance"),
                "ravi", 1L);

        assertEquals(new BigDecimal("35.00"), result.amountPaid());
        assertEquals(new BigDecimal("65.00"), result.outstandingBalance());
        assertEquals("PARTIALLY_PAID", result.paymentStatus());
        assertEquals(1, result.payments().size());
        verify(audits).save(argThat(a -> a.action.equals("PAYMENT_RECORDED")));
    }

    @Test
=======
>>>>>>> origin/main
    void singlePoInvoiceRequiresHeaderPo() {
        ApiException e = assertThrows(ApiException.class,
                () -> svc.create(request(DocType.TAX_INVOICE, " ", item(null)), "ravi"));
        assertEquals(HttpStatus.BAD_REQUEST, e.status);
    }

    @Test
    void multiPoInvoiceRequiresPoPerItemAndClearsHeaderPo() {
        assertThrows(ApiException.class,
                () -> svc.create(request(DocType.MULTI_PO_INVOICE, null, item(null)), "ravi"));
        BillDto d = svc.create(request(DocType.MULTI_PO_INVOICE, "IGNORED", item("6100008489")), "ravi");
        assertNull(d.poNumber());
        assertEquals("6100008489", d.items().get(0).poNumber());
    }

    @Test
    void finalizeAssignsNumberAndFreezesSeller() {
        BillDto d = svc.create(request(DocType.TAX_INVOICE, "1", item(null)), "ravi");
        Bill stored = new Bill();
        stored.id = d.id();
        stored.docType = DocType.TAX_INVOICE;
        stored.invoiceDate = d.invoiceDate();
        stored.currency = "INR";
        BillItem bi = new BillItem();
        bi.bill = stored; bi.qty = BigDecimal.TEN; bi.rate = BigDecimal.TEN;
        bi.cgstRate = new BigDecimal("9"); bi.sgstRate = new BigDecimal("9"); bi.name = "x"; bi.unit = "nos";
        stored.items.add(bi);
        stored.grandTotal = new BigDecimal("118.00");
        stored.buyerName = "B"; stored.buyerAddress = "A";
        when(bills.findById(1L)).thenReturn(Optional.of(stored));
        when(numbering.next(any(), any())).thenReturn("T-2026-27-0001");

        BillDto f = svc.finalizeBill(1L, "ravi");

        assertEquals(BillStatus.FINAL, f.status());
        assertEquals("T-2026-27-0001", f.invoiceNumber());
        assertNotNull(stored.sellerSnapshot);
        // later settings changes must not alter the finalized bill
        settings.companyName = "Renamed Co";
        assertEquals("Test Co", svc.get(1L).seller().companyName());
    }

    @Test
    void finalizedBillCannotBeEditedFinalizedAgainOrDeleted() {
        Bill b = new Bill();
        b.id = 5L;
        b.status = BillStatus.FINAL;
        b.docType = DocType.TAX_INVOICE;
        b.invoiceNumber = "T-1";
        when(bills.findById(5L)).thenReturn(Optional.of(b));
        BillRequest r = request(DocType.TAX_INVOICE, "1", item(null));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> svc.update(5L, r, "u")).status);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> svc.finalizeBill(5L, "u")).status);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> svc.deleteDraft(5L, "u")).status);
        assertEquals("T-1", b.invoiceNumber);
    }

    @Test
    void cancelOnlyAppliesToFinalizedBillsAndKeepsNumber() {
        Bill draft = new Bill();
        draft.id = 6L; draft.status = BillStatus.DRAFT; draft.docType = DocType.TAX_INVOICE;
        when(bills.findById(6L)).thenReturn(Optional.of(draft));
        assertThrows(ApiException.class, () -> svc.cancel(6L, "wrong buyer", "admin"));

        Bill fin = new Bill();
        fin.id = 7L; fin.status = BillStatus.FINAL; fin.docType = DocType.TAX_INVOICE; fin.invoiceNumber = "T-7";
        when(bills.findById(7L)).thenReturn(Optional.of(fin));
        BillDto c = svc.cancel(7L, "wrong buyer", "admin");
        assertEquals(BillStatus.CANCELLED, c.status());
        assertEquals("T-7", c.invoiceNumber());
        assertEquals("wrong buyer", c.cancelReason());
    }

    @Test
    void duplicateCreatesNewDraftAndLeavesOriginalUntouched() {
        Bill src = new Bill();
        src.id = 9L; src.status = BillStatus.FINAL; src.docType = DocType.TAX_INVOICE; src.invoiceNumber = "T-9";
        src.invoiceDate = LocalDate.of(2026, 1, 1); src.currency = "INR"; src.buyerName = "B"; src.buyerAddress = "A";
        src.poNumber = "55";
        BillItem si = new BillItem();
        si.bill = src; si.name = "x"; si.unit = "nos"; si.qty = BigDecimal.ONE; si.rate = BigDecimal.TEN;
        si.cgstRate = new BigDecimal("9"); si.sgstRate = new BigDecimal("9"); si.lineNo = 1;
        src.items.add(si);
        when(bills.findById(9L)).thenReturn(Optional.of(src));
        when(bills.save(any(Bill.class))).thenAnswer(inv -> { Bill b = inv.getArgument(0); b.id = 10L; return b; });

        BillDto d = svc.duplicate(9L, "ravi");

        assertEquals(BillStatus.DRAFT, d.status());
        assertNull(d.invoiceNumber());
        assertEquals(9L, d.duplicatedFrom());
        assertEquals("T-9", src.invoiceNumber);
        assertEquals(BillStatus.FINAL, src.status);
    }
}
