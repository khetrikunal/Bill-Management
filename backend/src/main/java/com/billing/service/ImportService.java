package com.billing.service;

import com.billing.domain.CompanySettings;
import com.billing.domain.Product;
import com.billing.dto.Dtos.ImportResult;
import com.billing.dto.Dtos.ImportRow;
import com.billing.repo.ProductRepository;
import com.billing.repo.SettingsRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Imports the PRODUCT list (not invoices) from an Excel file, with preview.
 * The supplied workbook holds only a template invoice and a draft item sheet with no quantities or totals,
 * so no invoice history is imported; its item lines are offered as product master data instead.
 * Modes: "legacy" reads the workbook's own item layout (name + "(code)" line, HSN col F, unit col H, rate col I,
 * CGST col K); "table" reads a header row with Name, Item Code, HSN, Unit, Rate, CGST %, SGST %.
 */
@Service
public class ImportService {
    private final ProductRepository products;
    private final SettingsRepository settings;
    private final DataFormatter fmt = new DataFormatter();

    public ImportService(ProductRepository products, SettingsRepository settings) {
        this.products = products;
        this.settings = settings;
    }

    @Transactional
    public ImportResult run(InputStream in, String mode, String sheetName, boolean commit) {
        CompanySettings s = settings.findById(1L).orElseThrow();
        List<ImportRow> parsed;
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = sheetName != null && !sheetName.isBlank() ? wb.getSheet(sheetName) : wb.getSheetAt(0);
            if (sheet == null) throw ApiException.badRequest("Sheet not found: " + sheetName);
            parsed = "legacy".equals(mode) ? legacy(sheet, s) : table(sheet, s);
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException a) throw a;
            throw ApiException.badRequest("Could not read the Excel file (.xlsx expected).");
        }

        List<ImportRow> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int nw = 0, dup = 0, err = 0, created = 0;
        for (ImportRow r : parsed) {
            String status = r.status(), msg = r.message();
            if (status.equals("NEW")) {
                String key = (r.itemCode() != null && !r.itemCode().isBlank() ? r.itemCode() : r.name() + "|" + r.hsn()).toLowerCase();
                boolean exists = r.itemCode() != null && !r.itemCode().isBlank()
                        ? !products.findByItemCodeIgnoreCase(r.itemCode()).isEmpty()
                        : !products.findByNameIgnoreCaseAndHsn(r.name(), r.hsn() == null ? "" : r.hsn()).isEmpty();
                if (exists || !seen.add(key)) {
                    status = "DUPLICATE";
                    msg = "Already exists / repeated in file - skipped";
                }
            }
            switch (status) {
                case "NEW" -> nw++;
                case "DUPLICATE" -> dup++;
                default -> err++;
            }
            if (commit && status.equals("NEW")) {
                Product p = new Product();
                p.name = r.name();
                p.itemCode = r.itemCode() == null ? "" : r.itemCode();
                p.hsn = r.hsn() == null ? "" : r.hsn();
                p.unit = r.unit();
                p.rate = r.rate();
                p.cgstRate = r.cgstRate();
                p.sgstRate = r.sgstRate();
                products.save(p);
                created++;
            }
            out.add(new ImportRow(r.row(), r.name(), r.itemCode(), r.hsn(), r.unit(), r.rate(), r.cgstRate(),
                    r.sgstRate(), status, msg));
        }
        return new ImportResult(out, nw, dup, err, commit, created);
    }

    // ---- legacy layout of the supplied workbook
    private List<ImportRow> legacy(Sheet sh, CompanySettings s) {
        List<ImportRow> rows = new ArrayList<>();
        int start = 0;
        for (int i = sh.getFirstRowNum(); i <= sh.getLastRowNum(); i++) {
            Row r = sh.getRow(i);
            if (r != null && "sr. no.".equalsIgnoreCase(text(r, 0))) { start = i + 1; break; }
        }
        int lastIdx = -1;
        for (int i = start; i <= sh.getLastRowNum(); i++) {
            Row r = sh.getRow(i);
            if (r == null) continue;
            String name = text(r, 1);
            String hsn = text(r, 5);
            BigDecimal rate = num(r, 8);
            if (name.isBlank()) continue;
            if (rate != null || !hsn.isBlank()) {
                BigDecimal cg = pctVal(num(r, 10), s.defaultCgstRate);
                BigDecimal sg = pctVal(num(r, 12), s.defaultSgstRate);
                String unit = text(r, 7).toLowerCase();
                rows.add(new ImportRow(i + 1, name, "", hsn, unit.isBlank() ? "nos" : unit, rate == null ? BigDecimal.ZERO : rate, cg, sg, "NEW", ""));
                lastIdx = rows.size() - 1;
            } else if (lastIdx >= 0 && !name.toUpperCase().replace(" ", "").startsWith("PO")) {
                ImportRow p = rows.get(lastIdx);
                if (p.itemCode() == null || p.itemCode().isEmpty()) {
                    rows.set(lastIdx, new ImportRow(p.row(), p.name(), name.replaceAll("^\\(|\\)$", "").trim(), p.hsn(),
                            p.unit(), p.rate(), p.cgstRate(), p.sgstRate(), "NEW", ""));
                }
            }
        }
        return validate(rows);
    }

    // ---- header-row layout
    private List<ImportRow> table(Sheet sh, CompanySettings s) {
        int hr = -1;
        Map<String, Integer> col = new HashMap<>();
        for (int i = sh.getFirstRowNum(); i <= Math.min(sh.getLastRowNum(), sh.getFirstRowNum() + 10); i++) {
            Row r = sh.getRow(i);
            if (r == null) continue;
            Map<String, Integer> m = new HashMap<>();
            for (Cell c : r) {
                String h = fmt.formatCellValue(c).toLowerCase().trim();
                if (h.contains("name") || h.contains("product") || h.contains("description")) m.putIfAbsent("name", c.getColumnIndex());
                else if (h.contains("code")) m.putIfAbsent("code", c.getColumnIndex());
                else if (h.contains("hsn") || h.contains("sac")) m.putIfAbsent("hsn", c.getColumnIndex());
                else if (h.contains("unit")) m.putIfAbsent("unit", c.getColumnIndex());
                else if (h.contains("rate") || h.contains("price")) m.putIfAbsent("rate", c.getColumnIndex());
                else if (h.contains("cgst")) m.putIfAbsent("cgst", c.getColumnIndex());
                else if (h.contains("sgst")) m.putIfAbsent("sgst", c.getColumnIndex());
            }
            if (m.containsKey("name")) { hr = i; col = m; break; }
        }
        if (hr < 0) throw ApiException.badRequest("Header row with a 'Name' column was not found in the first 10 rows.");
        List<ImportRow> rows = new ArrayList<>();
        for (int i = hr + 1; i <= sh.getLastRowNum(); i++) {
            Row r = sh.getRow(i);
            if (r == null) continue;
            String name = text(r, col.get("name"));
            if (name.isBlank()) continue;
            String unit = col.containsKey("unit") ? text(r, col.get("unit")).toLowerCase() : "";
            rows.add(new ImportRow(i + 1, name, col.containsKey("code") ? text(r, col.get("code")) : "",
                    col.containsKey("hsn") ? text(r, col.get("hsn")) : "", unit.isBlank() ? "nos" : unit,
                    col.containsKey("rate") ? num(r, col.get("rate")) : null,
                    pctVal(col.containsKey("cgst") ? num(r, col.get("cgst")) : null, s.defaultCgstRate),
                    pctVal(col.containsKey("sgst") ? num(r, col.get("sgst")) : null, s.defaultSgstRate), "NEW", ""));
        }
        return validate(rows);
    }

    private List<ImportRow> validate(List<ImportRow> rows) {
        List<ImportRow> out = new ArrayList<>();
        for (ImportRow r : rows) {
            String err = null;
            if (r.name().length() > 300) err = "Name too long";
            else if (r.hsn() != null && !r.hsn().isEmpty() && !r.hsn().matches("\\d{4,8}")) err = "HSN must be 4 to 8 digits";
            else if (r.rate() == null) err = "Rate missing";
            else if (r.rate().signum() < 0) err = "Rate cannot be negative";
            out.add(err == null ? r : new ImportRow(r.row(), r.name(), r.itemCode(), r.hsn(), r.unit(), r.rate(),
                    r.cgstRate(), r.sgstRate(), "ERROR", err));
        }
        return out;
    }

    /** The workbook stores 9% as 0.09 (percent cell format); a plain 9 means 9%. */
    private static BigDecimal pctVal(BigDecimal v, BigDecimal dflt) {
        if (v == null) return dflt;
        return v.compareTo(BigDecimal.ONE) <= 0 ? v.movePointRight(2).stripTrailingZeros() : v;
    }

    private String text(Row r, int c) {
        Cell cell = r.getCell(c);
        return cell == null ? "" : fmt.formatCellValue(cell).trim();
    }

    private BigDecimal num(Row r, int c) {
        Cell cell = r.getCell(c);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
            String s = fmt.formatCellValue(cell).replace(",", "").replace("%", "").trim();
            return s.isEmpty() ? null : new BigDecimal(s);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
