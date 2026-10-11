package com.billing.service;

import com.billing.domain.BillStatus;
import com.billing.domain.DocType;
import com.billing.dto.Dtos.BillDto;
import com.billing.dto.Dtos.ItemDto;
import com.billing.dto.Dtos.SellerSnapshot;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;

/** Renders the invoice as a text-selectable PDF that follows the layout of the Excel "Tax Invoice" sheet. */
@Service
public class PdfService {
    private static final Color HEAD_BG = new Color(189, 215, 238);
    private static final Color GREY = new Color(215, 212, 212);
    private static final Font F8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font F8B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font F9B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font F11B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font F13B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font F8I = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

    public static String copyLabel(String key) {
        if (key == null) return "(ORIGINAL FOR RECIPIENT)";
        return switch (key.toUpperCase()) {
            case "DUPLICATE" -> "(DUPLICATE FOR TRANSPORTER)";
            case "TRIPLICATE" -> "(TRIPLICATE FOR SUPPLIER)";
            default -> "(ORIGINAL FOR RECIPIENT)";
        };
    }

    public static String fileName(BillDto b) {
        String base = b.invoiceNumber() != null ? b.invoiceNumber() : "DRAFT-" + b.id();
        return base.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
    }

    public byte[] render(BillDto b, String copyKey) {
        SellerSnapshot s = b.seller();
        Rectangle size;
        try {
            size = PageSize.getRectangle(s.pageSize() == null ? "A4" : s.pageSize());
        } catch (RuntimeException e) {
            size = PageSize.A4;
        }
        boolean inr = "INR".equals(b.currency());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(size, 28, 28, 28, 28);
        try {
            PdfWriter w = PdfWriter.getInstance(doc, out);
            String mark = b.status() == BillStatus.DRAFT ? "DRAFT" : b.status() == BillStatus.CANCELLED ? "CANCELLED" : null;
            w.setPageEvent(new Watermark(mark));
            doc.addTitle(b.displayNumber());
            doc.addAuthor(s.companyName());
            doc.open();

            PdfPTable t = new PdfPTable(12);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{3.5f, 26f, 8f, 6f, 5f, 7f, 10f, 5f, 8f, 5f, 8f, 10f});
            t.setHeaderRows(5);
            t.setSplitRows(false);

            // title row (no border, like the sheet)
            t.addCell(cell("Tax Invoice", F13B, 8, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
            t.addCell(cell(copyLabel(copyKey), F8I, 4, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));

            // seller block + invoice no/date
            PdfPCell seller = new PdfPCell();
            seller.setColspan(8);
            seller.setPadding(4);
            Image logo = logo(s.logo());
            if (logo != null) {
                logo.scaleToFit(48, 48);
                Paragraph lp = new Paragraph();
                lp.add(new Chunk(logo, 0, -2));
                seller.addElement(lp);
            }
            seller.addElement(new Paragraph(s.companyName(), F11B));
            seller.addElement(new Paragraph(nz(s.address()), F8));
            if (!nz(s.phone()).isBlank()) seller.addElement(new Paragraph("Mobile: " + s.phone(), F8));
            if (!nz(s.email()).isBlank()) seller.addElement(new Paragraph("Mail-Id: " + s.email(), F8));
            if (!nz(s.gstin()).isBlank()) seller.addElement(new Paragraph("GSTIN: " + s.gstin(), F8B));
            t.addCell(seller);

            PdfPCell inv = new PdfPCell();
            inv.setColspan(4);
            inv.setPadding(4);
            inv.addElement(new Paragraph("Invoice No.", F8));
            inv.addElement(new Paragraph(b.invoiceNumber() != null ? b.invoiceNumber() : "DRAFT", F9B));
            Paragraph dt = new Paragraph("Invoice Date:", F8);
            dt.setSpacingBefore(4);
            inv.addElement(dt);
            inv.addElement(new Paragraph(b.invoiceDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), F9B));
            t.addCell(inv);

            // buyer block + P.O.
            PdfPCell buyer = new PdfPCell();
            buyer.setColspan(8);
            buyer.setPadding(4);
            buyer.addElement(new Paragraph("BUYER:", F8));
            buyer.addElement(new Paragraph(b.buyerName(), F11B));
            buyer.addElement(new Paragraph(nz(b.buyerAddress()), F8));
            if (!nz(b.buyerGstin()).isBlank()) buyer.addElement(new Paragraph("GSTIN: " + b.buyerGstin(), F8B));
            t.addCell(buyer);

            PdfPCell po = new PdfPCell();
            po.setColspan(4);
            po.setPadding(4);
            if (b.docType() == DocType.TAX_INVOICE) {
                po.addElement(new Paragraph("P.O. No:", F8));
                po.addElement(new Paragraph(nz(b.poNumber()), F11B));
            } else {
                po.addElement(new Paragraph("P.O. Nos:", F8));
                po.addElement(new Paragraph("As listed against items", F8));
            }
            if (b.transactionRef() != null) {
                Paragraph rf = new Paragraph("Ref: " + b.transactionRef(), F8);
                rf.setSpacingBefore(4);
                po.addElement(rf);
            }
            t.addCell(po);

            // item table header (two rows, like the sheet's CGST/SGST "Rate | Amount" sub-header)
            String[] h1 = {"Sr.\nNo.", "Name Of The Product", "HSN/SAC", "QTY", "Unit", "RATE", "Taxable\nValue"};
            for (String h : h1) t.addCell(head(h, 1, 2));
            t.addCell(head("CGST", 2, 1));
            t.addCell(head("SGST", 2, 1));
            t.addCell(head("Total", 1, 2));
            for (String h : new String[]{"Rate", "Amount", "Rate", "Amount"}) t.addCell(head(h, 1, 1));

            int lr = Rectangle.LEFT | Rectangle.RIGHT;
            String poGroup = null;
            int sr = 0, rows = 0;
            for (ItemDto i : b.items()) {
                if (b.docType() == DocType.MULTI_PO_INVOICE && !i.poNumber().equals(poGroup)) {
                    poGroup = i.poNumber();
                    t.addCell(cell("P.O. No: " + poGroup, F8B, 12, Element.ALIGN_LEFT, lr));
                    rows++;
                }
                sr++;
                rows++;
                String name = i.name() + (i.itemCode() == null ? "" : "\n(" + i.itemCode() + ")");
                t.addCell(cell(String.valueOf(sr), F8, 1, Element.ALIGN_CENTER, lr));
                t.addCell(cell(name, F8, 1, Element.ALIGN_LEFT, lr));
                t.addCell(cell(nz(i.hsn()), F8, 1, Element.ALIGN_CENTER, lr));
                t.addCell(cell(i.qty().stripTrailingZeros().toPlainString(), F8, 1, Element.ALIGN_RIGHT, lr));
                t.addCell(cell(i.unit(), F8, 1, Element.ALIGN_CENTER, lr));
                t.addCell(cell(money(i.rate(), inr), F8, 1, Element.ALIGN_RIGHT, lr));
                PdfPCell tx = cell(money(i.taxable(), inr), F8, 1, Element.ALIGN_RIGHT, lr);
                tx.setBackgroundColor(GREY);
                t.addCell(tx);
                t.addCell(cell(pct(i.cgstRate()), F8, 1, Element.ALIGN_RIGHT, lr));
                t.addCell(cell(money(i.cgst(), inr), F8, 1, Element.ALIGN_RIGHT, lr));
                t.addCell(cell(pct(i.sgstRate()), F8, 1, Element.ALIGN_RIGHT, lr));
                t.addCell(cell(money(i.sgst(), inr), F8, 1, Element.ALIGN_RIGHT, lr));
                PdfPCell tot = cell(money(i.total(), inr), F8, 1, Element.ALIGN_RIGHT, lr);
                tot.setBackgroundColor(GREY);
                t.addCell(tot);
            }
            // filler so short invoices keep the sheet's tall item area
            PdfPCell filler = cell("", F8, 12, Element.ALIGN_LEFT, lr);
            filler.setMinimumHeight(Math.max(6f, 280f - rows * 24f));
            t.addCell(filler);

            // totals row
            t.addCell(cell("Total", F8B, 6, Element.ALIGN_LEFT, Rectangle.BOX));
            t.addCell(cell(money(b.totalTaxable(), inr), F8B, 1, Element.ALIGN_RIGHT, Rectangle.BOX));
            t.addCell(cell("", F8B, 1, Element.ALIGN_RIGHT, Rectangle.BOX));
            t.addCell(cell(money(b.totalCgst(), inr), F8B, 1, Element.ALIGN_RIGHT, Rectangle.BOX));
            t.addCell(cell("", F8B, 1, Element.ALIGN_RIGHT, Rectangle.BOX));
            t.addCell(cell(money(b.totalSgst(), inr), F8B, 1, Element.ALIGN_RIGHT, Rectangle.BOX));
            t.addCell(cell(money(b.grandTotal(), inr), F8B, 1, Element.ALIGN_RIGHT, Rectangle.BOX));

            // footer left: words, bank, declaration
            PdfPCell left = new PdfPCell();
            left.setColspan(8);
            left.setPadding(4);
            left.addElement(new Paragraph("Amount Chargeable (in words):", F8));
            left.addElement(new Paragraph(b.amountInWords(), F8B));
            Paragraph bk = new Paragraph("Bank Details:", F8B);
            bk.setSpacingBefore(8);
            left.addElement(bk);
            left.addElement(new Paragraph("Bank Name: " + nz(s.bankName()), F8));
            left.addElement(new Paragraph("A/C: " + nz(s.bankAccount()), F8));
            left.addElement(new Paragraph("Branch: " + nz(s.bankBranch()) + "     IFSC: " + nz(s.ifsc()), F8));
            Paragraph dc = new Paragraph("Declaration:\n" + nz(s.declaration()), F8);
            dc.setSpacingBefore(8);
            left.addElement(dc);
            t.addCell(left);

            // footer right: tax summary + signature
            PdfPTable sum = new PdfPTable(2);
            sum.setWidths(new float[]{62, 38});
            sumRow(sum, "Total Amount:", money(b.totalTaxable(), inr), true, false);
            sumRow(sum, "Add CGST:", money(b.totalCgst(), inr), false, false);
            sumRow(sum, "Add SGST:", money(b.totalSgst(), inr), false, false);
            sumRow(sum, "Tax Amount:", money(b.totalTax(), inr), true, false);
            sumRow(sum, "Total Amount with Tax:", money(b.grandTotal(), inr), true, true);
            PdfPCell sig = new PdfPCell(new Phrase("For, " + nz(s.signatureText()) + "\n\n\n\nSignature", F8));
            sig.setColspan(2);
            sig.setHorizontalAlignment(Element.ALIGN_CENTER);
            sig.setPadding(4);
            sig.setBorder(Rectangle.NO_BORDER);
            sum.addCell(sig);
            PdfPCell right = new PdfPCell(sum);
            right.setColspan(4);
            right.setPadding(0);
            t.addCell(right);

            doc.add(t);
            if (!nz(s.footerNote()).isBlank()) {
                Paragraph fn = new Paragraph(s.footerNote(), F8I);
                fn.setSpacingBefore(6);
                doc.add(fn);
            }
            if (b.status() == BillStatus.CANCELLED) {
                Paragraph c = new Paragraph("CANCELLED: " + nz(b.cancelReason()), F8B);
                c.setSpacingBefore(6);
                doc.add(c);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF generation failed", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    // ---- helpers
    private static PdfPCell cell(String text, Font f, int colspan, int align, int border) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, f));
        c.setColspan(colspan);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(border);
        c.setPadding(3);
        return c;
    }

    private static PdfPCell head(String text, int colspan, int rowspan) {
        PdfPCell c = cell(text, F8B, colspan, Element.ALIGN_CENTER, Rectangle.BOX);
        c.setRowspan(rowspan);
        c.setBackgroundColor(HEAD_BG);
        return c;
    }

    private static void sumRow(PdfPTable t, String label, String value, boolean bold, boolean shade) {
        Font f = bold ? F8B : F8;
        PdfPCell a = cell(label, f, 1, Element.ALIGN_LEFT, Rectangle.BOX);
        PdfPCell b = cell(value, f, 1, Element.ALIGN_RIGHT, Rectangle.BOX);
        if (shade) {
            a.setBackgroundColor(GREY);
            b.setBackgroundColor(GREY);
        }
        t.addCell(a);
        t.addCell(b);
    }

    private static Image logo(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/")) return null;
        try {
            return Image.getInstance(Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    static String pct(BigDecimal r) {
        BigDecimal x = r.stripTrailingZeros();
        if (x.scale() < 1) x = x.setScale(1, RoundingMode.UNNECESSARY);
        return x.toPlainString() + "%";
    }

    /** Indian (12,34,567.00) or international (1,234,567.00) digit grouping, always 2 decimals. */
    static String money(BigDecimal v, boolean indian) {
        BigDecimal x = v.setScale(2, RoundingMode.HALF_UP);
        String[] p = x.abs().toPlainString().split("\\.");
        String ip = p[0], g;
        if (indian && ip.length() > 3) {
            String rest = ip.substring(0, ip.length() - 3).replaceAll("(\\d)(?=(\\d\\d)+$)", "$1,");
            g = rest + "," + ip.substring(ip.length() - 3);
        } else {
            g = ip.replaceAll("(\\d)(?=(\\d{3})+$)", "$1,");
        }
        return (x.signum() < 0 ? "-" : "") + g + "." + p[1];
    }

    private static class Watermark extends PdfPageEventHelper {
        private final String text;
        Watermark(String text) { this.text = text; }

        @Override
        public void onEndPage(PdfWriter w, Document d) {
            if (text == null) return;
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
                PdfContentByte cb = w.getDirectContentUnder();
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(0.12f);
                cb.saveState();
                cb.setGState(gs);
                cb.beginText();
                cb.setFontAndSize(bf, 90);
                cb.setColorFill(Color.GRAY);
                cb.showTextAligned(Element.ALIGN_CENTER, text, w.getPageSize().getWidth() / 2,
                        w.getPageSize().getHeight() / 2, 45);
                cb.endText();
                cb.restoreState();
            } catch (Exception ignored) {
                // watermark is cosmetic; never fail the document because of it
            }
        }
    }
}
