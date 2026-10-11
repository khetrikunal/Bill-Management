import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties } from 'react'
import { money, pct } from '../lib/calc'
import type { DocType, Seller } from '../lib/types'

export interface PreviewItem {
  poNumber?: string | null; name: string; itemCode?: string | null; hsn?: string | null
  qty: number; unit: string; rate: number; cgstRate: number; sgstRate: number
  taxable: number; cgst: number; sgst: number; total: number
}
export interface PreviewData {
  docType: DocType
  number: string            // invoice number or "DRAFT"
  invoiceDate: string       // yyyy-mm-dd
  poNumber?: string | null
  transactionRef?: string | null
  buyerName: string; buyerAddress: string; buyerGstin: string
  currency: string
  seller: Seller
  items: PreviewItem[]
  totals: { taxable: number; cgst: number; sgst: number; tax: number; grand: number }
  amountInWords: string
  status?: 'DRAFT' | 'FINAL' | 'CANCELLED'
  cancelReason?: string | null
}

const COPY: Record<string, string> = {
  ORIGINAL: '(ORIGINAL FOR RECIPIENT)',
  DUPLICATE: '(DUPLICATE FOR TRANSPORTER)',
  TRIPLICATE: '(TRIPLICATE FOR SUPPLIER)',
}
const PAGE_W = 794 // A4 at 96 dpi; the sheet is laid out at this width, then scaled to fit the screen

const fmtDate = (iso: string) => (iso ? iso.split('-').reverse().join('/') : '')
const b = '1px solid #222'

function useFitScale() {
  const wrap = useRef<HTMLDivElement>(null)
  const inner = useRef<HTMLDivElement>(null)
  const [scale, setScale] = useState(1)
  const [h, setH] = useState(0)
  useLayoutEffect(() => {
    const el = wrap.current
    if (!el) return
    const update = () => setScale(Math.min(1, el.clientWidth / PAGE_W))
    update()
    const ro = new ResizeObserver(update)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])
  useEffect(() => {
    const el = inner.current
    if (!el) return
    const ro = new ResizeObserver(() => setH(el.offsetHeight))
    ro.observe(el)
    return () => ro.disconnect()
  }, [])
  return { wrap, inner, scale, h }
}

/** HTML replica of the Excel "Tax Invoice" sheet. Shown on screen (scaled to fit phones) and printed 1:1 on A4. */
export default function InvoicePreview({ data, copy = 'ORIGINAL' }: { data: PreviewData; copy?: string }) {
  const { wrap, inner, scale, h } = useFitScale()
  const s = data.seller
  const m = (n: number) => money(n, data.currency)
  const cell: CSSProperties = { borderLeft: b, borderRight: b, padding: '2px 4px', fontSize: 11, verticalAlign: 'top' }
  const th: CSSProperties = { border: b, background: '#bdd7ee', fontSize: 11, fontWeight: 700, textAlign: 'center', padding: '3px 2px' }
  const grey = '#d7d4d4'

  let sr = 0
  let group: string | null | undefined = undefined
  const rows = data.items.flatMap((i, idx) => {
    const out: JSX.Element[] = []
    if (data.docType === 'MULTI_PO_INVOICE' && i.poNumber !== group) {
      group = i.poNumber
      out.push(<tr key={`g${idx}`}><td colSpan={12} style={{ ...cell, fontWeight: 700 }}>P.O. No: {i.poNumber}</td></tr>)
    }
    sr++
    out.push(
      <tr key={idx}>
        <td style={{ ...cell, textAlign: 'center' }}>{sr}</td>
        <td style={{ ...cell, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{i.name}{i.itemCode ? `\n(${i.itemCode})` : ''}</td>
        <td style={{ ...cell, textAlign: 'center' }}>{i.hsn}</td>
        <td style={{ ...cell, textAlign: 'right' }}>{Number.isFinite(i.qty) ? i.qty : ''}</td>
        <td style={{ ...cell, textAlign: 'center' }}>{i.unit}</td>
        <td style={{ ...cell, textAlign: 'right' }}>{m(i.rate)}</td>
        <td style={{ ...cell, textAlign: 'right', background: grey }}>{m(i.taxable)}</td>
        <td style={{ ...cell, textAlign: 'right' }}>{pct(i.cgstRate)}</td>
        <td style={{ ...cell, textAlign: 'right' }}>{m(i.cgst)}</td>
        <td style={{ ...cell, textAlign: 'right' }}>{pct(i.sgstRate)}</td>
        <td style={{ ...cell, textAlign: 'right' }}>{m(i.sgst)}</td>
        <td style={{ ...cell, textAlign: 'right', background: grey }}>{m(i.total)}</td>
      </tr>,
    )
    return out
  })

  const widths = [3.5, 26, 8, 6, 5, 7, 10, 5, 8, 5, 8, 10]
  const mark = data.status === 'DRAFT' ? 'DRAFT' : data.status === 'CANCELLED' ? 'CANCELLED' : null

  return (
    <div ref={wrap} className="print-root w-full overflow-hidden" style={{ height: h * scale || undefined }}>
      <div ref={inner} className="invoice-scale relative bg-white text-black shadow ring-1 ring-slate-300 print:shadow-none print:ring-0"
        style={{ width: PAGE_W, transform: `scale(${scale})`, transformOrigin: 'top left', padding: 24, fontFamily: 'Calibri, Arial, sans-serif' }}>
        {mark && (
          <div className="pointer-events-none absolute inset-0 flex items-center justify-center" aria-hidden>
            <span style={{ fontSize: 110, fontWeight: 800, color: 'rgba(0,0,0,.08)', transform: 'rotate(-35deg)' }}>{mark}</span>
          </div>
        )}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
          <span style={{ flex: 1, textAlign: 'right', fontSize: 18, fontWeight: 700 }}>Tax Invoice</span>
          <span style={{ flex: 1, textAlign: 'right', fontSize: 11, fontStyle: 'italic' }}>{COPY[copy] ?? COPY.ORIGINAL}</span>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
          <colgroup>{widths.map((w, i) => <col key={i} style={{ width: `${w}%` }} />)}</colgroup>
          <tbody>
            <tr>
              <td colSpan={8} style={{ border: b, padding: 6, verticalAlign: 'top' }}>
                {s.logo && <img src={s.logo} alt="" style={{ maxHeight: 48, maxWidth: 120, marginBottom: 4 }} />}
                <div style={{ fontSize: 14, fontWeight: 700 }}>{s.companyName}</div>
                <div style={{ fontSize: 11, whiteSpace: 'pre-line' }}>{s.address}</div>
                {s.phone && <div style={{ fontSize: 11 }}>Mobile: {s.phone}</div>}
                {s.email && <div style={{ fontSize: 11 }}>Mail-Id: {s.email}</div>}
                {s.gstin && <div style={{ fontSize: 11, fontWeight: 700 }}>GSTIN: {s.gstin}</div>}
              </td>
              <td colSpan={4} style={{ border: b, padding: 6, verticalAlign: 'top', fontSize: 11 }}>
                <div>Invoice No.</div>
                <div style={{ fontWeight: 700, fontSize: 13 }}>{data.number}</div>
                <div style={{ marginTop: 6 }}>Invoice Date:</div>
                <div style={{ fontWeight: 700, fontSize: 13 }}>{fmtDate(data.invoiceDate)}</div>
              </td>
            </tr>
            <tr>
              <td colSpan={8} style={{ border: b, padding: 6, verticalAlign: 'top' }}>
                <div style={{ fontSize: 11 }}>BUYER:</div>
                <div style={{ fontSize: 14, fontWeight: 700, wordBreak: 'break-word' }}>{data.buyerName}</div>
                <div style={{ fontSize: 11, whiteSpace: 'pre-line', wordBreak: 'break-word' }}>{data.buyerAddress}</div>
                {data.buyerGstin && <div style={{ fontSize: 11, fontWeight: 700 }}>GSTIN: {data.buyerGstin}</div>}
              </td>
              <td colSpan={4} style={{ border: b, padding: 6, verticalAlign: 'top', fontSize: 11 }}>
                {data.docType === 'TAX_INVOICE' ? (<><div>P.O. No:</div><div style={{ fontWeight: 700, fontSize: 14 }}>{data.poNumber}</div></>)
                  : (<><div>P.O. Nos:</div><div>As listed against items</div></>)}
                {data.transactionRef && <div style={{ marginTop: 6 }}>Ref: {data.transactionRef}</div>}
              </td>
            </tr>

            <tr>
              <th rowSpan={2} style={th}>Sr. No.</th><th rowSpan={2} style={th}>Name Of The Product</th>
              <th rowSpan={2} style={th}>HSN/SAC</th><th rowSpan={2} style={th}>QTY</th><th rowSpan={2} style={th}>Unit</th>
              <th rowSpan={2} style={th}>RATE</th><th rowSpan={2} style={th}>Taxable Value</th>
              <th colSpan={2} style={th}>CGST</th><th colSpan={2} style={th}>SGST</th><th rowSpan={2} style={th}>Total</th>
            </tr>
            <tr>{['Rate', 'Amount', 'Rate', 'Amount'].map((t, i) => <th key={i} style={th}>{t}</th>)}</tr>

            {rows}
            <tr><td colSpan={12} style={{ ...cell, height: Math.max(8, 260 - data.items.length * 26) }} /></tr>

            <tr style={{ fontWeight: 700 }}>
              <td colSpan={6} style={{ ...cell, border: b }}>Total</td>
              <td style={{ ...cell, border: b, textAlign: 'right' }}>{m(data.totals.taxable)}</td>
              <td style={{ ...cell, border: b }} />
              <td style={{ ...cell, border: b, textAlign: 'right' }}>{m(data.totals.cgst)}</td>
              <td style={{ ...cell, border: b }} />
              <td style={{ ...cell, border: b, textAlign: 'right' }}>{m(data.totals.sgst)}</td>
              <td style={{ ...cell, border: b, textAlign: 'right' }}>{m(data.totals.grand)}</td>
            </tr>

            <tr>
              <td colSpan={8} style={{ border: b, padding: 6, verticalAlign: 'top', fontSize: 11 }}>
                <div>Amount Chargeable (in words):</div>
                <div style={{ fontWeight: 700 }}>{data.amountInWords}</div>
                <div style={{ fontWeight: 700, marginTop: 10 }}>Bank Details:</div>
                <div>Bank Name: {s.bankName}</div>
                <div>A/C: {s.bankAccount}</div>
                <div>Branch: {s.bankBranch} &nbsp;&nbsp; IFSC: {s.ifsc}</div>
                <div style={{ marginTop: 10, whiteSpace: 'pre-line' }}>Declaration:{'\n'}{s.declaration}</div>
              </td>
              <td colSpan={4} style={{ border: b, padding: 0, verticalAlign: 'top', fontSize: 11 }}>
                {([['Total Amount:', data.totals.taxable, true, false], ['Add CGST:', data.totals.cgst, false, false],
                  ['Add SGST:', data.totals.sgst, false, false], ['Tax Amount:', data.totals.tax, true, false],
                  ['Total Amount with Tax:', data.totals.grand, true, true]] as const).map(([l, v, bold, shade]) => (
                  <div key={l} style={{ display: 'flex', justifyContent: 'space-between', borderBottom: b, padding: '2px 4px', fontWeight: bold ? 700 : 400, background: shade ? grey : undefined }}>
                    <span>{l}</span><span>{m(v)}</span>
                  </div>
                ))}
                <div style={{ textAlign: 'center', padding: '6px 4px', whiteSpace: 'pre-line' }}>
                  For, {s.signatureText}{'\n\n\n\n'}Signature
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        {s.footerNote && <p style={{ fontSize: 10, fontStyle: 'italic', marginTop: 6 }}>{s.footerNote}</p>}
        {data.status === 'CANCELLED' && <p style={{ fontSize: 11, fontWeight: 700, marginTop: 6 }}>CANCELLED: {data.cancelReason}</p>}
      </div>
    </div>
  )
}
