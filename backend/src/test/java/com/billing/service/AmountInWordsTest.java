package com.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AmountInWordsTest {
    private static String inr(String v) { return AmountInWords.convert(new BigDecimal(v), "INR"); }

    @Test void sheetExample() { assertEquals("INR One Thousand Four Hundred Sixteen Only", inr("1416")); }
    @Test void zero() { assertEquals("INR Zero Only", inr("0")); }
    @Test void paise() { assertEquals("INR One Thousand Four Hundred Sixteen and Fifty Paise Only", inr("1416.50")); }
    @Test void teensAndTens() { assertEquals("INR Nineteen Only", inr("19")); assertEquals("INR Ninety Nine Only", inr("99")); }
    @Test void lakhs() { assertEquals("INR Twelve Lakh Thirty Four Thousand Five Hundred Sixty Seven Only", inr("1234567")); }
    @Test void exactCrore() { assertEquals("INR One Crore Only", inr("10000000")); }
    @Test void largeCrore() { assertEquals("INR One Hundred Twenty Three Crore Only", inr("1230000000")); }
    @Test void internationalCurrency() {
        assertEquals("USD One Million Two Hundred Thirty Four Thousand Five Hundred Sixty Seven and Five Cents Only",
                AmountInWords.convert(new BigDecimal("1234567.05"), "USD"));
    }
}
