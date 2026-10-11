package com.billing.domain;

/**
 * The two document types found in the supplied workbook.
 * Sheet1 = TAX_INVOICE (one P.O. number in the header).
 * Sheet2 = MULTI_PO_INVOICE (same column layout, items grouped under several P.O. numbers, qty/amounts filled per item).
 */
public enum DocType {
    TAX_INVOICE("Tax Invoice (single P.O.)",
            "Sheet1 format: one P.O. number in the header, one list of items."),
    MULTI_PO_INVOICE("Tax Invoice (multiple P.O.s)",
            "Sheet2 format: same columns, items grouped under several P.O. numbers.");

    public final String label;
    public final String description;

    DocType(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
