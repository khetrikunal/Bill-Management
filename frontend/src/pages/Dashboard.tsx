import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, PageHeader, Spinner, StatusBadge } from '../components/ui'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'
import { money } from '../lib/calc'
import type { BillSummary } from '../lib/types'

interface Dash {
  total: number; thisMonth: number; drafts: number; finalized: number; cancelled: number
  recent: BillSummary[]; finalizedThisMonth: Array<{ currency: string; total: number }>
}

export default function Dashboard() {
  const { can } = useAuth()
  const [d, setD] = useState<Dash | null>(null)
  const [err, setErr] = useState('')
  useEffect(() => { api.get<Dash>('/dashboard').then(setD).catch(e => setErr(e.message)) }, [])
  if (err) return <Alert>{err}</Alert>
  if (!d) return <Spinner />

  const stats: Array<[string, number]> = [['Total bills', d.total], ['Created this month', d.thisMonth], ['Drafts', d.drafts], ['Finalized', d.finalized], ['Cancelled', d.cancelled]]
  return (
    <div>
      <PageHeader title="Dashboard">
        {can('ADMIN', 'BILLING') && (<>
          <Link className="btn btn-primary" to="/bills/new?type=TAX_INVOICE">+ Tax Invoice (single P.O.)</Link>
          <Link className="btn" to="/bills/new?type=MULTI_PO_INVOICE">+ Tax Invoice (multiple P.O.s)</Link>
        </>)}
      </PageHeader>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
        {stats.map(([l, v]) => <div key={l} className="card"><div className="text-sm text-slate-500">{l}</div><div className="text-3xl font-bold">{v}</div></div>)}
      </div>
      {d.finalizedThisMonth.length > 0 && (
        <div className="card mt-3">
          <div className="text-sm text-slate-500">Finalized this month (by invoice date)</div>
          <div className="mt-1 flex flex-wrap gap-x-6 text-xl font-bold">{d.finalizedThisMonth.map(t => <span key={t.currency}>{t.currency} {money(t.total, t.currency)}</span>)}</div>
        </div>
      )}
      <h2 className="mb-2 mt-6 text-lg font-semibold">Recent bills</h2>
      <div className="card divide-y p-0 sm:p-0">
        {d.recent.length === 0 && <p className="p-4 text-slate-500">No bills yet.</p>}
        {d.recent.map(b => (
          <Link key={b.id} to={`/bills/${b.id}`} className="flex items-center justify-between gap-3 p-4 hover:bg-slate-50">
            <div className="min-w-0"><div className="font-semibold">{b.displayNumber}</div><div className="truncate text-sm text-slate-600">{b.buyerName}</div></div>
            <div className="text-right"><div className="font-semibold">{b.currency} {money(b.grandTotal, b.currency)}</div><StatusBadge status={b.status} /></div>
          </Link>
        ))}
      </div>
    </div>
  )
}
