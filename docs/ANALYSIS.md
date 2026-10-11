# Phase 1 – Analysis of the supplied workbook (blank_bill_sset.xlsx)

Only the Excel file was supplied; **no PDF reference was attached**. The layout below was reconstructed by rendering the workbook itself.

## What the files actually are
* **Not an import-export invoice.** Sheet1 is a domestic **GST "Tax Invoice"** (INR, GSTIN, HSN/SAC, CGST + SGST, "ORIGINAL FOR RECIPIENT"). No export fields (shipping, destination, currency, duty) exist, so none were invented. Currency is stored per bill and defaults to INR, so foreign currency can still be used.
* **Sheet1 – single P.O. Tax Invoice.** Seller block (Shree Siddhnath Enterprises & Transport, Baramati, GSTIN 27CZOPK6914Q1ZE), invoice no. (`#00` placeholder), date (28/04/2026), buyer (Kalyani Technoforge Ltd.), one P.O. no. (6100015187), item table, totals, amount in words, bank details, declaration, signature.
* **Sheet2 – multi-P.O. item sheet.** Same 15-column table, but with 17+ items grouped under P.O. 6100008489 and P.O. 6100016278, quantities blank, no seller/buyer header, no invoice number, totals empty. It is a working sheet for a longer bill, not a finished invoice.

## Formulas (Excel tables Table2 / Table22)
| Column | Formula |
|---|---|
| Taxable Value | QTY × RATE |
| CGST / SGST amount | Taxable × rate (rate stored as 0.09 and formatted as %) |
| Total | Taxable + CGST + SGST |
| Totals row | SUM of each column |
| Tax Amount | CGST + SGST; Total with Tax = Total Amount + Tax Amount |

## Differences / ambiguities found
1. The table's stored calculated-column formula divides by 100 (`Taxable*Rate/100`), but the cells hold 9% as 0.09 and the printed values (108 on 1200) show the formula is not actually applied. The app uses Taxable × (rate % / 100), which reproduces every value in the file.
2. Sheet1 line values are typed constants, not formulas; Sheet2 has none.
3. Excel does no rounding. The app rounds every line to 2 decimals (half-up).
4. Amount in words on Sheet1 is typed by hand ("ONETHOUSAND FOUR  HUNDRED SIXTEEN"); the app generates it. Header typo "Invoive Date" is printed correctly as "Invoice Date".
5. Sheet2 has GST 2.5% + 2.5% on some items (Plaster of Paris, Cotton Waste) and 9% + 9% on others; rates are per line.
6. Sheet2's total row contains a stray label "9".
7. "(ORIGINAL FOR RECIPIENT)" suggests Duplicate/Triplicate copies; these are selectable when printing.

## How the two sheets map to the app
| Sheet | Document type | Difference |
|---|---|---|
| Sheet1 | `TAX_INVOICE` | One P.O. number in the header, required |
| Sheet2 | `MULTI_PO_INVOICE` | P.O. number per item, grouped under "P.O. No:" rows; header P.O. is never stored |

Both share seller/buyer header, columns, calculations and numbering. The optional **Reference / transaction no.** field links related bills. Assumption to confirm: Sheet2 would use the same seller/buyer header as Sheet1.

## Data import
Neither sheet holds real transactions (Sheet1 is a template with one sample line, Sheet2 has no quantities), so **no invoices are imported**. Sheet2's item lines can be imported as **products** (Products page → Import, "Your workbook layout") with a preview first.
