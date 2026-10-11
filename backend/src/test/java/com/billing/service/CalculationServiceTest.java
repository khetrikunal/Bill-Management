package com.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.billing.domain.Bill;
import com.billing.domain.BillItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculationServiceTest {
    private final CalculationService calc = new CalculationService();

    private static BillItem item(String qty, String rate, String cgst, String sgst) {
        BillItem i = new BillItem();
        i.qty = new BigDecimal(qty);
        i.rate = new BigDecimal(rate);
        i.cgstRate = new BigDecimal(cgst);
        i.sgstRate = new BigDecimal(sgst);
        return i;
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(new BigDecimal(expected), actual);
    }

    /** Sheet1 of the workbook: Nirma 30 kg x 40 @ 9% + 9% => 1200 / 108 / 108 / 1416. */
    @Test
    void matchesWorkbookSheet1() {
        Bill b = new Bill();
        b.currency = "INR";
        b.items.add(item("30", "40", "9", "9"));
        calc.calculate(b);
        BillItem i = b.items.get(0);
        assertMoney("1200.00", i.taxable);
        assertMoney("108.00", i.cgst);
        assertMoney("108.00", i.sgst);
        assertMoney("1416.00", i.total);
        assertMoney("1200.00", b.totalTaxable);
        assertMoney("216.00", b.totalTax);
        assertMoney("1416.00", b.grandTotal);
        assertEquals("INR One Thousand Four Hundred Sixteen Only", b.amountInWords);
    }

    @Test
    void handlesTwoAndHalfPercentRates() {
        BillItem i = item("2", "13", "2.5", "2.5");
        calc.calcLine(i);
        assertMoney("26.00", i.taxable);
        assertMoney("0.65", i.cgst);
        assertMoney("0.65", i.sgst);
        assertMoney("27.30", i.total);
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        BillItem i = item("3", "33.33", "9", "9"); // taxable 99.99, tax 8.9991 -> 9.00
        calc.calcLine(i);
        assertMoney("99.99", i.taxable);
        assertMoney("9.00", i.cgst);
        assertMoney("9.00", i.sgst);
        assertMoney("117.99", i.total);
    }

    @Test
    void totalsAreSumOfRoundedLines() {
        Bill b = new Bill();
        b.currency = "INR";
        b.items.add(item("1", "280", "9", "9"));   // 280 + 25.20 + 25.20
        b.items.add(item("12", "48", "9", "9"));   // 576 + 51.84 + 51.84
        b.items.add(item("2", "13", "2.5", "2.5"));
        calc.calculate(b);
        assertMoney("882.00", b.totalTaxable);
        assertMoney("77.69", b.totalCgst);
        assertMoney("77.69", b.totalSgst);
        assertMoney("155.38", b.totalTax);
        assertMoney("1037.38", b.grandTotal);
    }
}
