import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import InvoicePreview, { type PreviewData } from '../components/InvoicePreview'
import { Alert, PageHeader, Spinner, StatusBadge } from '../components/ui'
import { api, saveBlob, shareOrSave } from '../lib/api'
import { useAuth } from '../lib/auth'
import type { Bill } from '../lib/types'

interface Audit { action: string; username: string; detail: string; createdAt: string }

export function toPreview(b: Bill): PreviewData {
  return {
    docType: b.docType, number: b.invoiceNumber ?? 'DRAFT', invoiceDate: b.invoiceDate, poNumber: b.poNumber,
    transactionRef: b.transactionRef, buyerName: b.buyerName, buyerAddress: b.buyerAddress, buyerGstin: b.buyerGstin,
    currency: b.currency, seller: b.seller,
    items: b.items.map(i => ({ ...i, taxable: i.taxable ?? 0, cgst: i.cgst ?? 0, sgst: i.sgst ?? 0, total: i.total ?? 0 })),
    totals: { taxable: b.totalTaxable, cgst: b.totalCgst, sgst: b.totalSgst, tax: b.totalTax, grand: b.grandTotal },
    amountInWords: b.amountInWords, status: b.status, cancelReason: b.cancelReason,
  }
}

export default function BillView() {
  const { id } = useParams()
  const [sp] = useSearchParams()
  const nav = useNavigate()
  const { can } = useAuth()
  const [bill, setBill] = useState<Bill | null>(null)
  const [history, setHistory] = useState<Audit[]>([])
  const [err, setErr] = useState('')
  const [msg, setMsg] = useState('')
  const [busy, setBusy] = useState(false)
  const [copy, setCopy] = useState('ORIGINAL')

  const load = useCallback(async () => {
    try {
      const [b, h] = await Promise.all([api.get<Bill>(`/bills/${id}`), api.get<Audit[]>(`/bills/${id}/history`)])
      setBill(b); setHistory(h)
    } catch (e) { setErr((e as Error).message) }
  }, [id])
  useEffect(() => { load() }, [load])

  // /bills/:id?print=1 opens the print dialog once the sheet is on screen
  useEffect(() => { if (bill && sp.get('print')) setTimeout(() => window.print(), 400) }, [bill, sp])

  async function run(fn: () => Promise<void>) {
    setBusy(true); setErr(''); setMsg('')
    try { await fn() } catch (e) { setErr((e as Error).message) } finally { setBusy(false) }
  }

  const pdf = async (share: boolean) => run(async () => {
    const { blob, filename } = await api.blob(`/bills/${id}/pdf?copy=${copy}`)
    if (share) await shareOrSave(blob, filename); else saveBlob(blob, filename)
  })

  if (err && !bill) return <Alert>{err}</Alert>
  if (!bill) return <Spinner />
  const canWrite = can('ADMIN', 'BILLING')

  return (
    <div>
      <PageHeader title={bill.displayNumber}>
        <StatusBadge status={bill.status} />
      </PageHeader>
      <div className="no-print mb-3 space-y-2">
        {err && <Alert>{err}</Alert>}
        {msg && <Alert kind="success">{msg}</Alert>}
        <div className="text-sm text-slate-600">{bill.docTypeLabel} · {bill.buyerName}{bill.transactionRef ? ` · Ref ${bill.transactionRef}` : ''}{bill.duplicatedFrom ? ` · copied from #${bill.duplicatedFrom}` : ''}</div>
        <div className="flex flex-wrap items-center gap-2">
          <select className="input !w-auto !py-2" value={copy} onChange={e => setCopy(e.target.value)} aria-label="Copy type">
            <option value="ORIGINAL">Original for Recipient</option><option value="DUPLICATE">Duplicate for Transporter</option><option value="TRIPLICATE">Triplicate for Supplier</option>
          </select>
          <button className="btn btn-primary" disabled={busy} onClick={() => pdf(false)}>Download PDF</button>
          <button className="btn" disabled={busy} onClick={() => pdf(true)}>Share / Save to phone</button>
          <button className="btn" onClick={() => window.print()}>Print</button>
          {canWrite && bill.status === 'DRAFT' && <Link className="btn" to={`/bills/${bill.id}/edit`}>Edit</Link>}
          {canWrite && bill.status === 'DRAFT' && (
            <button className="btn btn-primary" disabled={busy} onClick={() => {
              if (!confirm('Finalize this bill? It will get its permanent invoice number and can no longer be edited.')) return
              run(async () => { await api.post(`/bills/${id}/finalize`); setMsg('Bill finalized.'); await load() })
            }}>Finalize</button>
          )}
          {canWrite && <button className="btn" disabled={busy} onClick={() => run(async () => { const d = await api.post<Bill>(`/bills/${id}/duplicate`); nav(`/bills/${d.id}/edit`) })}>Duplicate</button>}
          {can('ADMIN') && bill.status === 'FINAL' && (
            <button className="btn text-red-700" disabled={busy} onClick={() => {
              const reason = prompt('Reason for cancelling this bill (required):')
              if (!reason || reason.trim().length < 3) return
              run(async () => { await api.post(`/bills/${id}/cancel`, { reason }); await load() })
            }}>Cancel bill</button>
          )}
          {canWrite && bill.status === 'DRAFT' && (
            <button className="btn text-red-700" disabled={busy} onClick={() => {
              if (!confirm('Delete this draft permanently?')) return
              run(async () => { await api.del(`/bills/${id}`); nav('/bills') })
            }}>Delete draft</button>
          )}
        </div>
      </div>

      <InvoicePreview data={toPreview(bill)} copy={copy} />

      <section className="no-print card mt-6">
        <h2 className="mb-2 text-lg font-semibold">History</h2>
        <ul className="space-y-1 text-sm">
          {history.map((h, i) => (
            <li key={i}><span className="text-slate-500">{new Date(h.createdAt).toLocaleString()}</span> · <b>{h.action}</b> by {h.username}{h.detail ? ` — ${h.detail}` : ''}</li>
          ))}
        </ul>
      </section>
    </div>
  )
}
