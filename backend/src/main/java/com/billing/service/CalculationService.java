package com.billing.service;

import com.billing.domain.Bill;
import com.billing.domain.BillItem;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mirrors the workbook's table formulas:
 *   Taxable = QTY x RATE
 *   CGST    = Taxable x CGST% / 100        (the sheet stores 9% as 0.09 and multiplies; result is identical)
 *   SGST    = Taxable x SGST% / 100
 *   Total   = Taxable + CGST + SGST
 * Totals row = SUM of each column; Tax Amount = CGST + SGST; Total with Tax = Taxable total + Tax Amount.
 * Excel does not round; we round every line value to 2 decimals (HALF_UP) so stored amounts always match the printed ones.
 */
@Component
public class CalculationService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static BigDecimal r2(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    public void calcLine(BillItem i) {
        i.taxable = r2(i.qty.multiply(i.rate));
        i.cgst = r2(i.taxable.multiply(i.cgstRate).divide(HUNDRED, 6, RoundingMode.HALF_UP));
        i.sgst = r2(i.taxable.multiply(i.sgstRate).divide(HUNDRED, 6, RoundingMode.HALF_UP));
        i.total = i.taxable.add(i.cgst).add(i.sgst);
    }

    public void calculate(Bill b) {
        BigDecimal tx = BigDecimal.ZERO, cg = BigDecimal.ZERO, sg = BigDecimal.ZERO;
        for (BillItem i : b.items) {
            calcLine(i);
            tx = tx.add(i.taxable);
            cg = cg.add(i.cgst);
            sg = sg.add(i.sgst);
        }
        b.totalTaxable = r2(tx);
        b.totalCgst = r2(cg);
        b.totalSgst = r2(sg);
        b.totalTax = b.totalCgst.add(b.totalSgst);
        b.grandTotal = b.totalTaxable.add(b.totalTax);
        b.amountInWords = AmountInWords.convert(b.grandTotal, b.currency);
    }
}
