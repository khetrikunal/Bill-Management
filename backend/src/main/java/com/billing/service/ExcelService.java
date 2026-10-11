package com.billing.service;

import com.billing.dto.Dtos.BillDto;
import com.billing.dto.Dtos.ItemDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelService {
    public byte[] exportBills(List<BillDto> bills) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            Sheet s = wb.createSheet("Bills");
            String[] h = {"Invoice No", "Status", "Document Type", "Invoice Date", "Buyer", "Buyer GSTIN", "P.O. No",
                    "Ref", "Currency", "Taxable Value", "CGST", "SGST", "Tax Amount", "Total with Tax", "Created At"};
            row(s, 0, head, (Object[]) h);
            int r = 1;
            for (BillDto b : bills) {
                Row row = s.createRow(r++);
                put(row, 0, b.displayNumber(), null);
                put(row, 1, b.status().name(), null);
                put(row, 2, b.docTypeLabel(), null);
                put(row, 3, b.invoiceDate().toString(), null);
                put(row, 4, b.buyerName(), null);
                put(row, 5, b.buyerGstin(), null);
                put(row, 6, b.poNumber(), null);
                put(row, 7, b.transactionRef(), null);
                put(row, 8, b.currency(), null);
                put(row, 9, b.totalTaxable().doubleValue(), money);
                put(row, 10, b.totalCgst().doubleValue(), money);
                put(row, 11, b.totalSgst().doubleValue(), money);
                put(row, 12, b.totalTax().doubleValue(), money);
                put(row, 13, b.grandTotal().doubleValue(), money);
                put(row, 14, b.createdAt().toString(), null);
            }
            for (int c = 0; c < h.length; c++) s.setColumnWidth(c, 18 * 256);

            Sheet li = wb.createSheet("Line Items");
            String[] h2 = {"Invoice No", "P.O. No", "Sr", "Product", "Item Code", "HSN/SAC", "Qty", "Unit", "Rate",
                    "Taxable Value", "CGST %", "CGST", "SGST %", "SGST", "Total"};
            row(li, 0, head, (Object[]) h2);
            int lr = 1;
            for (BillDto b : bills) {
                int sr = 1;
                for (ItemDto i : b.items()) {
                    Row row = li.createRow(lr++);
                    put(row, 0, b.displayNumber(), null);
                    put(row, 1, i.poNumber() != null ? i.poNumber() : b.poNumber(), null);
                    put(row, 2, (double) sr++, null);
                    put(row, 3, i.name(), null);
                    put(row, 4, i.itemCode(), null);
                    put(row, 5, i.hsn(), null);
                    put(row, 6, i.qty().doubleValue(), null);
                    put(row, 7, i.unit(), null);
                    put(row, 8, i.rate().doubleValue(), money);
                    put(row, 9, i.taxable().doubleValue(), money);
                    put(row, 10, i.cgstRate().doubleValue(), null);
                    put(row, 11, i.cgst().doubleValue(), money);
                    put(row, 12, i.sgstRate().doubleValue(), null);
                    put(row, 13, i.sgst().doubleValue(), money);
                    put(row, 14, i.total().doubleValue(), money);
                }
            }
            for (int c = 0; c < h2.length; c++) li.setColumnWidth(c, 16 * 256);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel export failed", e);
        }
    }

    private static void row(Sheet s, int idx, CellStyle style, Object... vals) {
        Row r = s.createRow(idx);
        for (int i = 0; i < vals.length; i++) put(r, i, vals[i], style);
    }

    private static void put(Row r, int col, Object v, CellStyle style) {
        Cell c = r.createCell(col);
        if (v instanceof Double d) c.setCellValue(d);
        else c.setCellValue(v == null ? "" : String.valueOf(v));
        if (style != null) c.setCellStyle(style);
    }
}
