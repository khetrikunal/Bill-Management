import { useEffect, useState, type ChangeEvent } from 'react'
import { Alert, Field, PageHeader, Spinner } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import type { Product } from '../lib/types'

interface ImportRow { row: number; name: string; itemCode: string; hsn: string; unit: string; rate: number; status: string; message: string }
interface ImportResult { rows: ImportRow[]; newCount: number; duplicateCount: number; errorCount: number; committed: boolean; created: number }
const blank: Product = { name: '', itemCode: '', hsn: '', unit: 'nos', rate: 0, cgstRate: 9, sgstRate: 9, active: true }

export default function Products() {
  const { can } = useAuth()
  const write = can('ADMIN', 'BILLING')
  const [list, setList] = useState<Product[] | null>(null)
  const [q, setQ] = useState('')
  const [form, setForm] = useState<Product | null>(null)
  const [err, setErr] = useState('')
  const [fe, setFe] = useState<Record<string, string>>({})
  const [file, setFile] = useState<File | null>(null)
  const [mode, setMode] = useState('legacy')
  const [sheet, setSheet] = useState('Sheet2')
  const [preview, setPreview] = useState<ImportResult | null>(null)

  const load = () => api.get<Product[]>(`/products?q=${encodeURIComponent(q)}`).then(setList).catch(e => setErr(e.message))
  useEffect(() => { const t = setTimeout(load, 250); return () => clearTimeout(t) }, [q]) // eslint-disable-line

  async function save() {
    if (!form) return
    setErr(''); setFe({})
    try {
      const body = { ...form, rate: Number(form.rate), cgstRate: Number(form.cgstRate), sgstRate: Number(form.sgstRate) }
      if (form.id) await api.put(`/products/${form.id}`, body); else await api.post('/products', body)
      setForm(null); load()
    } catch (e) { setErr((e as ApiError).message); setFe((e as ApiError).fieldErrors) }
  }
  async function remove(p: Product) {
    if (!confirm(`Delete ${p.name}?`)) return
    try { await api.del(`/products/${p.id}`); load() } catch (e) { setErr((e as Error).message) }
  }
  async function runImport(commit: boolean) {
    if (!file) return
    setErr('')
    const f = new FormData()
    f.append('file', file)
    try {
      const r = await api.upload<ImportResult>(`/products/import?mode=${mode}&commit=${commit}&sheet=${encodeURIComponent(sheet)}`, f)
      setPreview(r)
      if (commit) load()
    } catch (e) { setErr((e as Error).message) }
  }
  const num = (k: 'rate' | 'cgstRate' | 'sgstRate') => (e: ChangeEvent<HTMLInputElement>) => setForm(f => f && { ...f, [k]: e.target.value as unknown as number })

  return (
    <div>
      <PageHeader title="Products">{write && <button className="btn btn-primary" onClick={() => setForm({ ...blank })}>+ Add product</button>}</PageHeader>
      {err && <Alert>{err}</Alert>}
      <input className="input my-3" placeholder="Search products" value={q} onChange={e => setQ(e.target.value)} />
      {form && (
        <div className="card mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Field label="Name" error={fe.name} className="col-span-2 sm:col-span-2"><input className="input" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="Item code" className="col-span-2"><input className="input" value={form.itemCode} onChange={e => setForm({ ...form, itemCode: e.target.value })} /></Field>
          <Field label="HSN/SAC" error={fe.hsn}><input className="input" value={form.hsn} onChange={e => setForm({ ...form, hsn: e.target.value })} /></Field>
          <Field label="Unit"><input className="input" value={form.unit} onChange={e => setForm({ ...form, unit: e.target.value })} /></Field>
          <Field label="Rate"><input className="input" inputMode="decimal" value={form.rate} onChange={num('rate')} /></Field>
          <Field label="CGST %"><input className="input" inputMode="decimal" value={form.cgstRate} onChange={num('cgstRate')} /></Field>
          <Field label="SGST %"><input className="input" inputMode="decimal" value={form.sgstRate} onChange={num('sgstRate')} /></Field>
          <div className="col-span-2 flex gap-2 sm:col-span-4"><button className="btn btn-primary" onClick={save}>Save</button><button className="btn" onClick={() => setForm(null)}>Cancel</button></div>
        </div>
      )}
      {!list ? <Spinner /> : (
        <div className="card divide-y p-0 sm:p-0">
          {list.length === 0 && <p className="p-4 text-slate-500">No products yet. Add one or import from Excel below.</p>}
          {list.map(p => (
            <div key={p.id} className="flex items-center justify-between gap-3 p-3">
              <div className="min-w-0"><div className="font-semibold">{p.name}</div><div className="text-sm text-slate-600">{p.itemCode && `${p.itemCode} · `}HSN {p.hsn || '-'} · {p.unit} · rate {p.rate} · {p.cgstRate}%+{p.sgstRate}%</div></div>
              {write && <div className="flex shrink-0 gap-2"><button className="btn" onClick={() => setForm(p)}>Edit</button><button className="btn text-red-700" onClick={() => remove(p)}>Delete</button></div>}
            </div>
          ))}
        </div>
      )}

      {write && (
        <section className="card mt-6 space-y-3">
          <h2 className="text-lg font-semibold">Import products from Excel</h2>
          <p className="text-sm text-slate-600">Imports the product list only, never invoices. Choose "Your workbook layout" to read item lines from a sheet laid out like your current bill (Sheet2). Nothing is saved until you press Import.</p>
          <div className="grid gap-3 sm:grid-cols-3">
            <Field label="Layout"><select className="input" value={mode} onChange={e => setMode(e.target.value)}><option value="legacy">Your workbook layout</option><option value="table">Table with header row</option></select></Field>
            <Field label="Sheet name"><input className="input" value={sheet} onChange={e => setSheet(e.target.value)} /></Field>
            <Field label="Excel file (.xlsx)"><input type="file" accept=".xlsx" className="input" onChange={e => { setFile(e.target.files?.[0] ?? null); setPreview(null) }} /></Field>
          </div>
          <button className="btn" disabled={!file} onClick={() => runImport(false)}>Preview</button>
          {preview && (
            <div className="space-y-2">
              <Alert kind={preview.committed ? 'success' : 'info'}>{preview.committed ? `Imported ${preview.created} product(s). ` : ''}New: {preview.newCount} · Duplicates skipped: {preview.duplicateCount} · Errors: {preview.errorCount}</Alert>
              <div className="max-h-80 overflow-auto rounded-lg border">
                <table className="w-full text-left text-sm"><thead className="sticky top-0 bg-slate-100"><tr><th className="p-2">Row</th><th className="p-2">Name</th><th className="p-2">HSN</th><th className="p-2">Rate</th><th className="p-2">Status</th></tr></thead>
                  <tbody className="divide-y">{preview.rows.map(r => <tr key={r.row}><td className="p-2">{r.row}</td><td className="p-2">{r.name}{r.itemCode && ` (${r.itemCode})`}</td><td className="p-2">{r.hsn}</td><td className="p-2">{r.rate}</td><td className="p-2">{r.status}{r.message && `: ${r.message}`}</td></tr>)}</tbody></table>
              </div>
              {!preview.committed && preview.newCount > 0 && <button className="btn btn-primary" onClick={() => runImport(true)}>Import {preview.newCount} new product(s)</button>}
            </div>
          )}
        </section>
      )}
    </div>
  )
}
