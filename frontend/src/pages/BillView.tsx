import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import InvoicePreview, { type PreviewData } from '../components/InvoicePreview'
import { Alert, PageHeader, Spinner, StatusBadge } from '../components/ui'
import { api, saveBlob, shareOrSave } from '../lib/api'
import { useAuth } from '../lib/auth'
import type { Bill } from '../lib/types'
import { useCompany } from '../lib/company'

interface Audit { action: string; username: string; detail: string; createdAt: string }

export function toPreview(b: Bill): PreviewData {
  return {
    docType: b.docType, number: b.invoiceNumber ?? 'DRAFT', invoiceDate: b.invoiceDate, dueDate: b.dueDate,
    paymentTerms: b.paymentTerms, poNumber: b.poNumber,
    transactionRef: b.transactionRef, buyerName: b.buyerName, buyerAddress: b.buyerAddress, buyerGstin: b.buyerGstin,
    buyerPhone: b.buyerPhone, buyerEmail: b.buyerEmail, shippingAddress: b.shippingAddress,
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
  const { selectCompany } = useCompany()
  const [bill, setBill] = useState<Bill | null>(null)
  const [history, setHistory] = useState<Audit[]>([])
  const [err, setErr] = useState('')
  const [msg, setMsg] = useState('')
  const [busy, setBusy] = useState(false)
  const [copy, setCopy] = useState('ORIGINAL')
  const [paymentAmount, setPaymentAmount] = useState('')
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().slice(0, 10))
  const [paymentMethod, setPaymentMethod] = useState('BANK_TRANSFER')
  const [paymentReference, setPaymentReference] = useState('')
  const [paymentNotes, setPaymentNotes] = useState('')

  const load = useCallback(async () => {
    try {
      const selectedId = sp.get('companyId')
      const suffix = selectedId ? `?companyId=${selectedId}` : ''
      const [b, h] = await Promise.all([api.get<Bill>(`/bills/${id}${suffix}`), api.get<Audit[]>(`/bills/${id}/history${suffix}`)])
      selectCompany(b.companyId)
      setBill(b); setHistory(h)
    } catch (e) { setErr((e as Error).message) }
  }, [id, sp, selectCompany])
  useEffect(() => { load() }, [load])

  // /bills/:id?print=1 opens the print dialog once the sheet is on screen
  useEffect(() => { if (bill && sp.get('print')) setTimeout(() => window.print(), 400) }, [bill, sp])

  async function run(fn: () => Promise<void>) {
    setBusy(true); setErr(''); setMsg('')
    try { await fn() } catch (e) { setErr((e as Error).message) } finally { setBusy(false) }
  }

  const pdf = async (share: boolean) => run(async () => {
    const { blob, filename } = await api.blob(`/bills/${id}/pdf?copy=${copy}&companyId=${bill?.companyId ?? ''}`)
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
        <div className="text-sm text-slate-600">{bill.companyDisplayName} · {bill.docTypeLabel} · {bill.buyerName}{bill.transactionRef ? ` · Ref ${bill.transactionRef}` : ''}{bill.duplicatedFrom ? ` · copied from #${bill.duplicatedFrom}` : ''}</div>
        <div className="text-sm">Payment: <b>{bill.paymentStatus}</b> · Received {bill.currency} {bill.amountPaid.toFixed(2)} · Outstanding {bill.currency} {bill.outstandingBalance.toFixed(2)}</div>
        <div className="flex flex-wrap items-center gap-2">
          <select className="input !w-auto !py-2" value={copy} onChange={e => setCopy(e.target.value)} aria-label="Copy type">
            <option value="ORIGINAL">Original for Recipient</option><option value="DUPLICATE">Duplicate for Transporter</option><option value="TRIPLICATE">Triplicate for Supplier</option>
          </select>
          <button className="btn btn-primary" disabled={busy} onClick={() => pdf(false)}>Download PDF</button>
          <button className="btn" disabled={busy} onClick={() => pdf(true)}>Share / Save to phone</button>
          <button className="btn" onClick={() => window.print()}>Print</button>
          {canWrite && bill.status === 'DRAFT' && <Link className="btn" to={`/bills/${bill.id}/edit?companyId=${bill.companyId}`}>Edit</Link>}
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

      {canWrite && bill.status === 'FINAL' && bill.outstandingBalance > 0 && (
        <section className="no-print card mt-6">
          <h2 className="mb-3 text-lg font-semibold">Record payment</h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <label className="text-sm">Amount<input className="input mt-1" inputMode="decimal" value={paymentAmount} onChange={e => setPaymentAmount(e.target.value)} /></label>
            <label className="text-sm">Payment date<input className="input mt-1" type="date" value={paymentDate} onChange={e => setPaymentDate(e.target.value)} /></label>
            <label className="text-sm">Method<select className="input mt-1" value={paymentMethod} onChange={e => setPaymentMethod(e.target.value)}>{['CASH', 'BANK_TRANSFER', 'UPI', 'CHEQUE', 'OTHER'].map(m => <option key={m} value={m}>{m.replace('_', ' ')}</option>)}</select></label>
            <label className="text-sm">Transaction reference<input className="input mt-1" value={paymentReference} onChange={e => setPaymentReference(e.target.value)} /></label>
            <label className="text-sm sm:col-span-2">Notes<input className="input mt-1" value={paymentNotes} onChange={e => setPaymentNotes(e.target.value)} /></label>
          </div>
          <button className="btn btn-primary mt-3" disabled={busy || !paymentAmount} onClick={() => run(async () => {
            await api.post(`/bills/${id}/payments`, {
              amount: Number(paymentAmount), paymentDate, method: paymentMethod,
              transactionReference: paymentReference, notes: paymentNotes,
            })
            setPaymentAmount(''); setPaymentReference(''); setPaymentNotes('')
            setMsg('Payment recorded.'); await load()
          })}>Add payment</button>
        </section>
      )}
      {bill.payments.length > 0 && (
        <section className="no-print card mt-4">
          <h2 className="mb-2 text-lg font-semibold">Payment history</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead><tr><th className="py-2">Date</th><th>Method</th><th>Reference</th><th>Notes</th><th className="text-right">Amount</th></tr></thead>
              <tbody className="divide-y">{bill.payments.map(p => <tr key={p.id}>
                <td className="py-2">{p.paymentDate}</td><td>{p.method.replace('_', ' ')}</td>
                <td>{p.transactionReference}</td><td>{p.notes}</td><td className="text-right">{bill.currency} {p.amount.toFixed(2)}</td>
              </tr>)}</tbody>
            </table>
          </div>
        </section>
      )}

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
