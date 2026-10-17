import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, PageHeader, Spinner, StatusBadge } from '../components/ui'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'
import { money } from '../lib/calc'
import type { BillSummary } from '../lib/types'
import { useCompany } from '../lib/company'

interface Dash {
  total: number; thisMonth: number; drafts: number; finalized: number; cancelled: number; overdue: number
  recent: BillSummary[]; finalizedThisMonth: Array<{ currency: string; total: number }>
  companies: Array<{ companyId: number; companyDisplayName: string; invoiceCount: number; totals: Array<{
    currency: string; billed: number; received: number; outstanding: number
  }> }>
}

export default function Dashboard() {
  const { can } = useAuth()
  const { companies, companyId } = useCompany()
  const [companyFilter, setCompanyFilter] = useState(String(companyId))
  const [d, setD] = useState<Dash | null>(null)
  const [err, setErr] = useState('')
  useEffect(() => { setCompanyFilter(String(companyId)) }, [companyId])
  useEffect(() => {
    setD(null); setErr('')
    api.get<Dash>(`/dashboard?companyFilter=${encodeURIComponent(companyFilter)}`).then(setD).catch(e => setErr(e.message))
  }, [companyFilter])
  if (err) return <Alert>{err}</Alert>
  if (!d) return <Spinner />

  const stats: Array<[string, number]> = [['Total bills', d.total], ['Created this month', d.thisMonth], ['Drafts', d.drafts], ['Finalized', d.finalized], ['Overdue', d.overdue], ['Cancelled', d.cancelled]]
  return (
    <div>
      <PageHeader title="Dashboard">
        {can('ADMIN', 'BILLING') && (<>
          <Link className="btn btn-primary" to="/bills/new?type=TAX_INVOICE">+ Tax Invoice (single P.O.)</Link>
          <Link className="btn" to="/bills/new?type=MULTI_PO_INVOICE">+ Tax Invoice (multiple P.O.s)</Link>
        </>)}
      </PageHeader>
      <div className="card mb-3 max-w-md">
        <label className="text-sm font-semibold">Select Business / Company</label>
        <select className="input mt-1" value={companyFilter} onChange={e => setCompanyFilter(e.target.value)}>
          <option value="ALL">All businesses combined</option>
          {companies.map(company => <option key={company.id} value={company.id}>{company.displayName}</option>)}
        </select>
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
        {stats.map(([l, v]) => <div key={l} className="card"><div className="text-sm text-slate-500">{l}</div><div className="text-3xl font-bold">{v}</div></div>)}
      </div>
      {d.finalizedThisMonth.length > 0 && (
        <div className="card mt-3">
          <div className="text-sm text-slate-500">Finalized this month (by invoice date)</div>
          <div className="mt-1 flex flex-wrap gap-x-6 text-xl font-bold">{d.finalizedThisMonth.map(t => <span key={t.currency}>{t.currency} {money(t.total, t.currency)}</span>)}</div>
        </div>
      )}
      <section className="mt-5">
        <h2 className="mb-2 text-lg font-semibold">Business totals</h2>
        <div className="grid gap-3 lg:grid-cols-2">
          {d.companies.map(company => (
            <div className="card" key={company.companyId}>
              <div className="mb-2 flex items-center justify-between gap-2">
                <h3 className="font-semibold">{company.companyDisplayName}</h3>
                <span className="text-sm text-slate-500">{company.invoiceCount} finalized invoices</span>
              </div>
              {company.totals.length === 0 ? <p className="text-sm text-slate-500">No finalized invoices yet.</p> :
                company.totals.map(t => (
                  <div key={t.currency} className="grid grid-cols-3 gap-2 border-t py-2 text-sm">
                    <div><div className="text-slate-500">Billed</div><b>{t.currency} {money(t.billed, t.currency)}</b></div>
                    <div><div className="text-slate-500">Received</div><b>{t.currency} {money(t.received, t.currency)}</b></div>
                    <div><div className="text-slate-500">Outstanding</div><b>{t.currency} {money(t.outstanding, t.currency)}</b></div>
                  </div>
                ))}
            </div>
          ))}
        </div>
      </section>
      <h2 className="mb-2 mt-6 text-lg font-semibold">Recent bills</h2>
      <div className="card divide-y p-0 sm:p-0">
        {d.recent.length === 0 && <p className="p-4 text-slate-500">No bills yet.</p>}
        {d.recent.map(b => (
          <Link key={b.id} to={`/bills/${b.id}?companyId=${b.companyId}`} className="flex items-center justify-between gap-3 p-4 hover:bg-slate-50">
            <div className="min-w-0"><div className="font-semibold">{b.displayNumber}</div><div className="truncate text-sm text-slate-600">{b.companyDisplayName} · {b.buyerName}</div></div>
            <div className="text-right"><div className="font-semibold">{b.currency} {money(b.grandTotal, b.currency)}</div><StatusBadge status={b.status} /></div>
          </Link>
        ))}
      </div>
    </div>
  )
}
