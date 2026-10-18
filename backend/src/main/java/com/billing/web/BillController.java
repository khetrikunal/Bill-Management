package com.billing.web;

import com.billing.domain.BillStatus;
import com.billing.domain.DocType;
import com.billing.dto.Dtos.*;
import com.billing.service.BillService;
import com.billing.service.ExcelService;
import com.billing.service.PdfService;
import com.billing.service.CompanyAccessService;
import com.billing.service.ApiException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BillController {
    private static final String WRITE = "hasAnyRole('ADMIN','BILLING')";
    private final BillService svc;
    private final PdfService pdf;
    private final ExcelService excel;
    private final CompanyAccessService companyAccess;

    public BillController(BillService svc, PdfService pdf, ExcelService excel, CompanyAccessService companyAccess) {
        this.svc = svc;
        this.pdf = pdf;
        this.excel = excel;
        this.companyAccess = companyAccess;
    }

    @GetMapping("/document-types")
    public List<DocTypeDto> types() {
        return Arrays.stream(DocType.values()).map(t -> new DocTypeDto(t.name(), t.label, t.description)).toList();
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId,
                               @RequestParam(required = false) String companyFilter, Authentication authentication) {
        return svc.dashboard(filterCompany(companyFilter, companyId, authentication));
    }

    @GetMapping("/bills")
    public PageDto<BillSummary> list(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) DocType type,
                                     @RequestParam(required = false) BillStatus status,
                                     @RequestParam(required = false) String paymentStatus,
                                     @RequestParam(defaultValue = "createdAt") String sort,
                                     @RequestParam(defaultValue = "desc") String dir,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId,
                                     @RequestParam(required = false) String companyFilter, Authentication authentication) {
        return svc.search(q, from, to, type, status, paymentStatus, sort, dir, page, size,
                filterCompany(companyFilter, companyId, authentication));
    }

    @GetMapping("/bills/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) DocType type,
                                         @RequestParam(required = false) BillStatus status,
                                         @RequestParam(required = false) String paymentStatus,
                                         @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId,
                                         @RequestParam(required = false) String companyFilter, Authentication authentication) {
        byte[] data = excel.exportBills(svc.exportAll(q, from, to, type, status, paymentStatus,
                filterCompany(companyFilter, companyId, authentication)));
        return file(data, "bills-export.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), false);
    }

    @GetMapping("/bills/{id}")
    public BillDto get(@PathVariable Long id,
                       @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId,
                       Authentication authentication) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(authentication.getName(), selected);
        return svc.get(id, selected);
    }

    @GetMapping("/bills/{id}/history")
    public List<AuditDto> history(@PathVariable Long id,
                                  @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId,
                                  Authentication authentication) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(authentication.getName(), selected);
        return svc.history(id, selected);
    }

    @GetMapping("/bills/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @RequestParam(defaultValue = "ORIGINAL") String copy,
                                      @RequestParam(defaultValue = "false") boolean inline,
                                      @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId,
                                      Authentication authentication) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(authentication.getName(), selected);
        BillDto b = svc.get(id, selected);
        return file(pdf.render(b, copy), PdfService.fileName(b), MediaType.APPLICATION_PDF, inline);
    }

    @PostMapping("/bills")
    @PreAuthorize(WRITE)
    public BillDto create(@Valid @RequestBody BillRequest r, Authentication a,
                          @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(a.getName(), selected);
        return svc.create(r, a.getName(), selected);
    }

    @PutMapping("/bills/{id}")
    @PreAuthorize(WRITE)
    public BillDto update(@PathVariable Long id, @Valid @RequestBody BillRequest r, Authentication a,
                          @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(a.getName(), selected);
        return svc.update(id, r, a.getName(), selected);
    }

    @PostMapping("/bills/{id}/duplicate")
    @PreAuthorize(WRITE)
    public BillDto duplicate(@PathVariable Long id, Authentication a,
                             @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(a.getName(), selected);
        return svc.duplicate(id, a.getName(), selected);
    }

    @PostMapping("/bills/{id}/finalize")
    @PreAuthorize(WRITE)
    public BillDto finalizeBill(@PathVariable Long id, Authentication a,
                                @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(a.getName(), selected);
        return svc.finalizeBill(id, a.getName(), selected);
    }

    @PostMapping("/bills/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public BillDto cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest r, Authentication a,
                          @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(a.getName(), selected);
        return svc.cancel(id, r.reason(), a.getName(), selected);
    }

    @DeleteMapping("/bills/{id}")
    @PreAuthorize(WRITE)
    public void delete(@PathVariable Long id, Authentication a,
                       @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(a.getName(), selected);
        svc.deleteDraft(id, a.getName(), selected);
    }

    @PostMapping("/bills/{id}/payments")
    @PreAuthorize(WRITE)
    public BillDto payment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request, Authentication a,
                           @RequestHeader(value = "X-Company-Id", defaultValue = "1") String companyId) {
        Long selected = selectedCompany(companyId);
        companyAccess.requireAccess(a.getName(), selected);
        return svc.recordPayment(id, request, a.getName(), selected);
    }

    private Long filterCompany(String filter, String selected, Authentication authentication) {
        String value = filter == null || filter.isBlank() ? selected : filter;
        if ("ALL".equalsIgnoreCase(value)) {
            List<Long> allowed = companyAccess.companyIds(authentication.getName());
            if (allowed.isEmpty()) throw ApiException.forbidden("You are not authorized for any company.");
            return allowed.size() == 1 ? allowed.get(0) : null;
        }
        Long id = selectedCompany(value);
        companyAccess.requireAccess(authentication.getName(), id);
        return id;
    }

    private static Long selectedCompany(String value) {
        try {
            long id = Long.parseLong(value);
            if (id < 1) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw com.billing.service.ApiException.badRequest("Invalid company selection.");
        }
    }

    private ResponseEntity<byte[]> file(byte[] data, String name, MediaType type, boolean inline) {
        ContentDisposition cd = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(name).build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store").contentType(type).body(data);
    }
}
