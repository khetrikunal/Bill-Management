import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, Field, PageHeader, Spinner, StatusBadge } from '../components/ui'
import { api, saveBlob } from '../lib/api'
import { useAuth } from '../lib/auth'
import { money } from '../lib/calc'
import type { BillSummary, Page } from '../lib/types'
import { useCompany } from '../lib/company'

export default function BillList() {
  const { can } = useAuth()
  const { companies, companyId } = useCompany()
  const [q, setQ] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [type, setType] = useState('')
  const [status, setStatus] = useState('')
  const [paymentStatus, setPaymentStatus] = useState('')
  const [companyFilter, setCompanyFilter] = useState(String(companyId))
  const [sort, setSort] = useState('createdAt')
  const [dir, setDir] = useState('desc')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<BillSummary> | null>(null)
  const [err, setErr] = useState('')
  const [dq, setDq] = useState('')

  useEffect(() => { const t = setTimeout(() => { setDq(q); setPage(0) }, 300); return () => clearTimeout(t) }, [q])
  useEffect(() => { setCompanyFilter(String(companyId)) }, [companyId])

  const qs = useCallback((extra = '') => {
    const p = new URLSearchParams()
    if (dq) p.set('q', dq); if (from) p.set('from', from); if (to) p.set('to', to)
    if (type) p.set('type', type); if (status) p.set('status', status)
    if (paymentStatus) p.set('paymentStatus', paymentStatus)
    if (companyFilter) p.set('companyFilter', companyFilter)
    return p.toString() + extra
  }, [dq, from, to, type, status, paymentStatus, companyFilter])

  useEffect(() => {
    setErr('')
    api.get<Page<BillSummary>>(`/bills?${qs(`&sort=${sort}&dir=${dir}&page=${page}&size=15`)}`).then(setData).catch(e => setErr(e.message))
  }, [qs, sort, dir, page])

  async function pdf(id: number) {
    const bill = data?.content.find(item => item.id === id)
    try { const { blob, filename } = await api.blob(`/bills/${id}/pdf?companyId=${bill?.companyId ?? companyId}`); saveBlob(blob, filename) } catch (e) { setErr((e as Error).message) }
  }
  async function exportXlsx() {
    try { const { blob, filename } = await api.blob(`/bills/export?${qs()}`); saveBlob(blob, filename) } catch (e) { setErr((e as Error).message) }
  }
  const reset = () => { setQ(''); setFrom(''); setTo(''); setType(''); setStatus(''); setPaymentStatus(''); setCompanyFilter(String(companyId)); setPage(0) }
  const onFilter = (fn: (v: string) => void) => (e: { target: { value: string } }) => { fn(e.target.value); setPage(0) }

  return (
    <div>
      <PageHeader title="Bill Records">
        <button className="btn" onClick={exportXlsx}>Export to Excel</button>
        {can('ADMIN', 'BILLING') && <Link className="btn btn-primary" to="/bills/new">+ New bill</Link>}
      </PageHeader>
      <div className="card mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Field label="Search invoice no., buyer, P.O." className="lg:col-span-2"><input className="input" value={q} onChange={e => setQ(e.target.value)} placeholder="e.g. SSET-2026 or customer" /></Field>
        <Field label="Business / company"><select className="input" value={companyFilter} onChange={onFilter(setCompanyFilter)}><option value="ALL">All businesses</option>{companies.map(company => <option key={company.id} value={company.id}>{company.displayName}</option>)}</select></Field>
        <Field label="From date"><input type="date" className="input" value={from} onChange={onFilter(setFrom)} /></Field>
        <Field label="To date"><input type="date" className="input" value={to} onChange={onFilter(setTo)} /></Field>
        <Field label="Document type"><select className="input" value={type} onChange={onFilter(setType)}><option value="">All</option><option value="TAX_INVOICE">Single P.O.</option><option value="MULTI_PO_INVOICE">Multiple P.O.s</option></select></Field>
        <Field label="Status"><select className="input" value={status} onChange={onFilter(setStatus)}><option value="">All</option><option value="DRAFT">Draft</option><option value="FINAL">Finalized</option><option value="CANCELLED">Cancelled</option></select></Field>
        <Field label="Payment status"><select className="input" value={paymentStatus} onChange={onFilter(setPaymentStatus)}>
          <option value="">All payments</option><option value="UNPAID">Unpaid</option><option value="PARTIALLY_PAID">Partially paid</option>
          <option value="PAID">Paid</option><option value="OVERDUE">Overdue</option>
        </select></Field>
        <Field label="Sort by"><select className="input" value={sort} onChange={e => setSort(e.target.value)}><option value="createdAt">Created</option><option value="invoiceDate">Invoice date</option><option value="grandTotal">Amount</option><option value="invoiceNumber">Invoice no.</option></select></Field>
        <div className="flex items-end gap-2">
          <button className="btn flex-1" onClick={() => setDir(d => (d === 'desc' ? 'asc' : 'desc'))}>{dir === 'desc' ? '↓ Newest/Highest' : '↑ Oldest/Lowest'}</button>
          <button className="btn" onClick={reset}>Clear</button>
        </div>
      </div>
      {err && <Alert>{err}</Alert>}
      {!data ? <Spinner /> : (
        <>
          {/* phones: cards */}
          <div className="space-y-3 md:hidden">
            {data.content.map(b => (
              <div key={b.id} className="card">
                <div className="flex items-start justify-between gap-2">
                  <Link to={`/bills/${b.id}?companyId=${b.companyId}`} className="font-semibold text-navy-700 underline">{b.displayNumber}</Link><StatusBadge status={b.status} />
                </div>
                <div className="text-sm text-slate-600">{b.companyDisplayName} · {b.buyerName}</div>
                <div className="mt-1 flex justify-between text-sm"><span>{b.invoiceDate.split('-').reverse().join('/')}</span><b>{b.currency} {money(b.grandTotal, b.currency)}</b></div>
                <div className="mt-1 text-xs text-slate-600">Payment: {b.paymentStatus} · Outstanding {b.currency} {money(b.outstandingBalance, b.currency)}</div>
                <div className="mt-3 flex gap-2">
                  <Link className="btn flex-1" to={`/bills/${b.id}?companyId=${b.companyId}`}>View</Link>
                  <button className="btn flex-1" onClick={() => pdf(b.id)}>PDF</button>
                  <Link className="btn flex-1" to={`/bills/${b.id}?companyId=${b.companyId}&print=1`}>Print</Link>
                </div>
              </div>
            ))}
          </div>
          {/* desktop: table */}
          <div className="card hidden overflow-x-auto p-0 md:block sm:p-0">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-100 text-slate-600"><tr>
                {['Invoice no.', 'Company', 'Type', 'Buyer', 'Date', 'Currency', 'Total', 'Payment', 'Created', 'Status', 'Actions'].map(h => <th key={h} className="px-3 py-3 font-semibold">{h}</th>)}
              </tr></thead>
              <tbody className="divide-y">
                {data.content.map(b => (
                  <tr key={b.id} className="hover:bg-slate-50">
                    <td className="px-3 py-2 font-semibold"><Link className="text-navy-700 underline" to={`/bills/${b.id}?companyId=${b.companyId}`}>{b.displayNumber}</Link></td>
                    <td className="px-3 py-2">{b.companyDisplayName}</td>
                    <td className="px-3 py-2">{b.docTypeLabel}</td><td className="px-3 py-2">{b.buyerName}</td>
                    <td className="px-3 py-2">{b.invoiceDate.split('-').reverse().join('/')}</td><td className="px-3 py-2">{b.currency}</td>
                    <td className="px-3 py-2 text-right">{money(b.grandTotal, b.currency)}</td>
                    <td className="px-3 py-2">{b.paymentStatus}</td>
                    <td className="px-3 py-2">{new Date(b.createdAt).toLocaleDateString()}</td>
                    <td className="px-3 py-2"><StatusBadge status={b.status} /></td>
                    <td className="whitespace-nowrap px-3 py-2">
                      <Link className="mr-3 text-navy-700 underline" to={`/bills/${b.id}?companyId=${b.companyId}`}>View</Link>
                      <button className="mr-3 text-navy-700 underline" onClick={() => pdf(b.id)}>PDF</button>
                      <Link className="mr-3 text-navy-700 underline" to={`/bills/${b.id}?companyId=${b.companyId}&print=1`}>Print</Link>
                      {can('ADMIN', 'BILLING') && b.status === 'DRAFT' && <Link className="text-navy-700 underline" to={`/bills/${b.id}/edit?companyId=${b.companyId}`}>Edit</Link>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {data.content.length === 0 && <p className="p-6 text-center text-slate-500">No bills match these filters.</p>}
          <div className="mt-4 flex items-center justify-between text-sm">
            <span>{data.totalElements} bill(s)</span>
            <div className="flex items-center gap-2">
              <button className="btn" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</button>
              <span>Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
              <button className="btn" disabled={page + 1 >= data.totalPages} onClick={() => setPage(p => p + 1)}>Next</button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
