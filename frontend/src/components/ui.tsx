import type { ReactNode } from 'react'
import type { BillStatus } from '../lib/types'

export function Field({ label, error, children, className = '' }: { label: string; error?: string; children: ReactNode; className?: string }) {
  return (
    <div className={className}>
      <label className="label">{label}</label>
      {children}
      {error && <p className="err">{error}</p>}
    </div>
  )
}

const COLORS: Record<BillStatus, string> = {
  DRAFT: 'bg-amber-100 text-amber-800',
  FINAL: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-red-100 text-red-700',
}
export function StatusBadge({ status }: { status: BillStatus }) {
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${COLORS[status]}`}>{status === 'FINAL' ? 'Finalized' : status[0] + status.slice(1).toLowerCase()}</span>
}

export function Alert({ kind = 'error', children }: { kind?: 'error' | 'info' | 'success'; children: ReactNode }) {
  const c = kind === 'error' ? 'border-red-200 bg-red-50 text-red-800' : kind === 'success' ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-sky-200 bg-sky-50 text-sky-800'
  return <div className={`rounded-lg border px-4 py-3 text-sm ${c}`} role="alert">{children}</div>
}

export function Spinner() {
  return <div className="p-8 text-center text-slate-500">Loading…</div>
}

export function PageHeader({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="no-print mb-4 flex flex-wrap items-center justify-between gap-3">
      <h1 className="text-xl font-bold sm:text-2xl">{title}</h1>
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  )
}
