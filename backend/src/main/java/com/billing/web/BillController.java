package com.billing.web;

import com.billing.domain.BillStatus;
import com.billing.domain.DocType;
import com.billing.dto.Dtos.*;
import com.billing.service.BillService;
import com.billing.service.ExcelService;
import com.billing.service.PdfService;
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

    public BillController(BillService svc, PdfService pdf, ExcelService excel) {
        this.svc = svc;
        this.pdf = pdf;
        this.excel = excel;
    }

    @GetMapping("/document-types")
    public List<DocTypeDto> types() {
        return Arrays.stream(DocType.values()).map(t -> new DocTypeDto(t.name(), t.label, t.description)).toList();
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard() { return svc.dashboard(); }

    @GetMapping("/bills")
    public PageDto<BillSummary> list(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) DocType type,
                                     @RequestParam(required = false) BillStatus status,
                                     @RequestParam(defaultValue = "createdAt") String sort,
                                     @RequestParam(defaultValue = "desc") String dir,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return svc.search(q, from, to, type, status, sort, dir, page, size);
    }

    @GetMapping("/bills/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) DocType type,
                                         @RequestParam(required = false) BillStatus status) {
        byte[] data = excel.exportBills(svc.exportAll(q, from, to, type, status));
        return file(data, "bills-export.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), false);
    }

    @GetMapping("/bills/{id}")
    public BillDto get(@PathVariable Long id) { return svc.get(id); }

    @GetMapping("/bills/{id}/history")
    public List<AuditDto> history(@PathVariable Long id) { return svc.history(id); }

    @GetMapping("/bills/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @RequestParam(defaultValue = "ORIGINAL") String copy,
                                      @RequestParam(defaultValue = "false") boolean inline) {
        BillDto b = svc.get(id);
        return file(pdf.render(b, copy), PdfService.fileName(b), MediaType.APPLICATION_PDF, inline);
    }

    @PostMapping("/bills")
    @PreAuthorize(WRITE)
    public BillDto create(@Valid @RequestBody BillRequest r, Authentication a) { return svc.create(r, a.getName()); }

    @PutMapping("/bills/{id}")
    @PreAuthorize(WRITE)
    public BillDto update(@PathVariable Long id, @Valid @RequestBody BillRequest r, Authentication a) {
        return svc.update(id, r, a.getName());
    }

    @PostMapping("/bills/{id}/duplicate")
    @PreAuthorize(WRITE)
    public BillDto duplicate(@PathVariable Long id, Authentication a) { return svc.duplicate(id, a.getName()); }

    @PostMapping("/bills/{id}/finalize")
    @PreAuthorize(WRITE)
    public BillDto finalizeBill(@PathVariable Long id, Authentication a) { return svc.finalizeBill(id, a.getName()); }

    @PostMapping("/bills/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public BillDto cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest r, Authentication a) {
        return svc.cancel(id, r.reason(), a.getName());
    }

    @DeleteMapping("/bills/{id}")
    @PreAuthorize(WRITE)
    public void delete(@PathVariable Long id, Authentication a) { svc.deleteDraft(id, a.getName()); }

    private ResponseEntity<byte[]> file(byte[] data, String name, MediaType type, boolean inline) {
        ContentDisposition cd = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(name).build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store").contentType(type).body(data);
    }
}
