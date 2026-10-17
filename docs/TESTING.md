# Test plan

## Automated
* Backend: `cd backend && mvn test` – calculation (matches Sheet1: 1200 / 108 / 108 / 1416), rounding, amount in words, financial year + invoice number format, bill workflow (draft → finalize → cancel, finalized bills cannot be edited/deleted, duplicate leaves original untouched, seller snapshot frozen, type rules for single vs multi P.O.).
* Frontend: `cd frontend && npm test` – same formulas and words on the client.

## Manual checklist (run once after setup)
1. Create and finalize an SSET *single P.O.* bill → number uses the configured SSET prefix and financial year.
2. Create a *multiple P.O.* bill with two P.O. groups; check grouping on the PDF.
3. Compare each PDF with the Excel print (Calibri vs Helvetica font is the only expected difference).
4. Change qty/rate/GST % and confirm live totals; try qty 0, negative rate, bad HSN/GSTIN → rejected.
5. Refresh and log in from another device → bills still listed.
6. Download PDF; print from browser (A4, no browser headers); on a phone use "Share / Save to phone".
7. Long buyer name/address and 30+ items → no overflow, header repeats on page 2.
8. Finalize two bills quickly from two browsers → numbers are consecutive, no duplicates.
9. Edit a finalized bill → blocked; duplicate → new draft; cancel (admin) → number stays, PDF shows CANCELLED.
10. Log in as Viewer → no create/edit buttons, API write calls return 403.
11. Switch between SSET and PK Corporate. Confirm profiles load independently and old finalized invoices retain their seller snapshots.
12. Create and finalize a bill for each company. Confirm numbering sequences are independent, and a user without a company's access receives 403 for its records.
13. Record a partial payment and confirm balance/status/history; record the remainder. Test an overdue due date and the dashboard/list filters.
14. Products → Import → "Your workbook layout", sheet `Sheet2` → preview, then import.
15. Bill Records: search, company/date/type/invoice/payment-status filters, sort, pagination, Export to Excel.
