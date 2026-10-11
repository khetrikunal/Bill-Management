package com.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Amount in words. INR uses the Indian system (lakh/crore); other currencies use million/billion. */
public final class AmountInWords {
    private AmountInWords() { }

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    public static String convert(BigDecimal amount, String currency) {
        BigDecimal a = amount.setScale(2, RoundingMode.HALF_UP).abs();
        long whole = a.longValue();
        int minor = a.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        boolean inr = "INR".equals(currency);
        StringBuilder sb = new StringBuilder(currency == null ? "" : currency).append(' ');
        sb.append(whole == 0 ? "Zero" : (inr ? indian(whole) : international(whole)));
        if (minor > 0) {
            sb.append(" and ").append(below100(minor)).append(inr ? " Paise" : " Cents");
        }
        return sb.append(" Only").toString();
    }

    private static String below100(int n) {
        if (n < 20) return ONES[n];
        return TENS[n / 10] + (n % 10 > 0 ? " " + ONES[n % 10] : "");
    }

    private static String below1000(int n) {
        StringBuilder sb = new StringBuilder();
        if (n >= 100) {
            sb.append(ONES[n / 100]).append(" Hundred");
            n %= 100;
            if (n > 0) sb.append(' ');
        }
        if (n > 0) sb.append(below100(n));
        return sb.toString();
    }

    static String indian(long n) {
        StringBuilder sb = new StringBuilder();
        long[] div = {10_000_000L, 100_000L, 1_000L};
        String[] name = {"Crore", "Lakh", "Thousand"};
        for (int i = 0; i < div.length; i++) {
            long part = n / div[i];
            if (part > 0) {
                // crores can exceed 99, so recurse for that part
                sb.append(i == 0 ? indian(part) : below100((int) part)).append(' ').append(name[i]).append(' ');
                n %= div[i];
            }
        }
        if (n > 0) sb.append(below1000((int) n));
        return sb.toString().trim();
    }

    static String international(long n) {
        long[] div = {1_000_000_000_000L, 1_000_000_000L, 1_000_000L, 1_000L};
        String[] name = {"Trillion", "Billion", "Million", "Thousand"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < div.length; i++) {
            long part = n / div[i];
            if (part > 0) {
                sb.append(below1000((int) part)).append(' ').append(name[i]).append(' ');
                n %= div[i];
            }
        }
        if (n > 0) sb.append(below1000((int) n));
        return sb.toString().trim();
    }
}
