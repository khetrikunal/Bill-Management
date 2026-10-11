import { useEffect, useState } from 'react'
import { Alert, Field, PageHeader, Spinner } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import type { Customer } from '../lib/types'

const blank: Customer = { name: '', address: '', gstin: '', email: '', phone: '' }

export default function Customers() {
  const { can } = useAuth()
  const [list, setList] = useState<Customer[] | null>(null)
  const [q, setQ] = useState('')
  const [form, setForm] = useState<Customer | null>(null)
  const [err, setErr] = useState('')
  const [fe, setFe] = useState<Record<string, string>>({})

  const load = () => api.get<Customer[]>(`/customers?q=${encodeURIComponent(q)}`).then(setList).catch(e => setErr(e.message))
  useEffect(() => { const t = setTimeout(load, 250); return () => clearTimeout(t) }, [q]) // eslint-disable-line

  async function save() {
    if (!form) return
    setErr(''); setFe({})
    try {
      if (form.id) await api.put(`/customers/${form.id}`, form); else await api.post('/customers', form)
      setForm(null); load()
    } catch (e) { setErr((e as ApiError).message); setFe((e as ApiError).fieldErrors) }
  }
  async function remove(c: Customer) {
    if (!confirm(`Delete ${c.name}? Existing bills keep their own copy of the buyer details.`)) return
    try { await api.del(`/customers/${c.id}`); load() } catch (e) { setErr((e as Error).message) }
  }

  return (
    <div>
      <PageHeader title="Buyers">{can('ADMIN', 'BILLING') && <button className="btn btn-primary" onClick={() => setForm({ ...blank })}>+ Add buyer</button>}</PageHeader>
      {err && <Alert>{err}</Alert>}
      <input className="input mb-3" placeholder="Search buyers" value={q} onChange={e => setQ(e.target.value)} />
      {form && (
        <div className="card mb-4 grid gap-3 sm:grid-cols-2">
          <Field label="Name" error={fe.name}><input className="input" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="GSTIN" error={fe.gstin}><input className="input uppercase" value={form.gstin} onChange={e => setForm({ ...form, gstin: e.target.value.toUpperCase() })} /></Field>
          <Field label="Address" className="sm:col-span-2"><textarea className="input" rows={3} value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} /></Field>
          <Field label="Email"><input className="input" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></Field>
          <Field label="Phone"><input className="input" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} /></Field>
          <div className="flex gap-2 sm:col-span-2"><button className="btn btn-primary" onClick={save}>Save</button><button className="btn" onClick={() => setForm(null)}>Cancel</button></div>
        </div>
      )}
      {!list ? <Spinner /> : (
        <div className="card divide-y p-0 sm:p-0">
          {list.length === 0 && <p className="p-4 text-slate-500">No buyers saved yet.</p>}
          {list.map(c => (
            <div key={c.id} className="flex items-start justify-between gap-3 p-4">
              <div className="min-w-0"><div className="font-semibold">{c.name}</div><div className="whitespace-pre-line text-sm text-slate-600">{c.address}</div>{c.gstin && <div className="text-xs text-slate-500">GSTIN {c.gstin}</div>}</div>
              {can('ADMIN', 'BILLING') && <div className="flex shrink-0 gap-2"><button className="btn" onClick={() => setForm(c)}>Edit</button><button className="btn text-red-700" onClick={() => remove(c)}>Delete</button></div>}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
