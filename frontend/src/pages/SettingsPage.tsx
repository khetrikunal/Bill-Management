import { useEffect, useState, type ChangeEvent } from 'react'
import { Alert, Field, PageHeader, Spinner } from '../components/ui'
import { api, ApiError } from '../lib/api'
import type { Settings } from '../lib/types'
import { useCompany } from '../lib/company'

export default function SettingsPage() {
  const { companyId } = useCompany()
  const [s, setS] = useState<Settings | null>(null)
  const [err, setErr] = useState('')
  const [ok, setOk] = useState('')
  const [fe, setFe] = useState<Record<string, string>>({})
  useEffect(() => {
    setS(null); setErr('')
    api.get<Settings>('/settings').then(setS).catch(e => setErr(e.message))
  }, [companyId])
  if (!s) return err ? <Alert>{err}</Alert> : <Spinner />

  const set = <K extends keyof Settings>(k: K, v: Settings[K]) => setS({ ...s, [k]: v })
  const txt = (k: keyof Settings, label: string, extra = '') => (
    <Field label={label} error={fe[k]} className={extra}><input className="input" value={(s[k] as string) ?? ''} onChange={e => set(k, e.target.value as never)} /></Field>
  )
  function logo(e: ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (!f) return
    if (f.size > 300_000) { setErr('Logo must be under 300 KB'); return }
    const r = new FileReader()
    r.onload = () => set('logo', String(r.result))
    r.readAsDataURL(f)
  }
  async function save() {
    setErr(''); setOk(''); setFe({})
    try { setS(await api.put<Settings>('/settings', s)); setOk('Settings saved. New bills use them; finalized bills are unchanged.') }
    catch (e) { setErr((e as ApiError).message); setFe((e as ApiError).fieldErrors) }
  }
  const fy = '2026-27'
  const example = `${s.invoicePrefix}${s.includeFinancialYear ? '-' + fy : ''}-${'1'.padStart(s.numberPadding, '0')}`

  return (
    <div className="space-y-4">
      <PageHeader title="Company Settings"><button className="btn btn-primary" onClick={save}>Save settings</button></PageHeader>
      {err && <Alert>{err}</Alert>}{ok && <Alert kind="success">{ok}</Alert>}
      <section className="card grid gap-3 sm:grid-cols-2">
        <h2 className="text-lg font-semibold sm:col-span-2">Company</h2>
        {txt('companyName', 'Legal business name')}{txt('displayName', 'Display name')}
        {txt('gstin', 'GSTIN')}{txt('pan', 'PAN')}
        <Field label="Address" className="sm:col-span-2"><textarea className="input" rows={3} value={s.address} onChange={e => set('address', e.target.value)} /></Field>
        <Field label="Billing address" className="sm:col-span-2"><textarea className="input" rows={3} value={s.billingAddress} onChange={e => set('billingAddress', e.target.value)} /></Field>
        {txt('phone', 'Contact number')}{txt('email', 'Email')}
        <Field label="Logo (PNG/JPG, under 300 KB)" className="sm:col-span-2">
          <input type="file" accept="image/png,image/jpeg" onChange={logo} />
          {s.logo && <div className="mt-2 flex items-center gap-3"><img src={s.logo} alt="logo" className="h-12" /><button className="btn" onClick={() => set('logo', null)}>Remove</button></div>}
        </Field>
      </section>
      <section className="card grid gap-3 sm:grid-cols-2">
        <h2 className="text-lg font-semibold sm:col-span-2">Bank details</h2>
        {txt('bankName', 'Bank name')}{txt('bankAccount', 'Account number')}{txt('bankBranch', 'Branch')}{txt('ifsc', 'IFSC')}
        {txt('upiId', 'UPI ID')}{txt('defaultPaymentTerms', 'Default payment terms')}
      </section>
      <section className="card grid gap-3 sm:grid-cols-2">
        <h2 className="text-lg font-semibold sm:col-span-2">Printed text and authorized signatory</h2>
        <Field label="Declaration" className="sm:col-span-2"><textarea className="input" rows={3} value={s.declaration} onChange={e => set('declaration', e.target.value)} /></Field>
        {txt('signatureText', 'Signature line ("For, …")', 'sm:col-span-2')}
        {txt('authorizedSignatory', 'Authorized signatory')}{txt('signatoryDesignation', 'Designation')}
        <Field label="Footer note (optional)" className="sm:col-span-2"><textarea className="input" rows={2} value={s.footerNote} onChange={e => set('footerNote', e.target.value)} /></Field>
      </section>
      <section className="card grid gap-3 sm:grid-cols-2">
        <h2 className="text-lg font-semibold sm:col-span-2">Defaults and numbering</h2>
        {txt('defaultCurrency', 'Default currency (3 letters)')}
        <Field label="Page size"><select className="input" value={s.pageSize} onChange={e => set('pageSize', e.target.value)}>{['A4', 'A5', 'LETTER', 'LEGAL'].map(p => <option key={p}>{p}</option>)}</select></Field>
        <Field label="Default CGST %"><input className="input" inputMode="decimal" value={s.defaultCgstRate} onChange={e => set('defaultCgstRate', e.target.value as unknown as number)} /></Field>
        <Field label="Default SGST %"><input className="input" inputMode="decimal" value={s.defaultSgstRate} onChange={e => set('defaultSgstRate', e.target.value as unknown as number)} /></Field>
        {txt('invoicePrefix', 'Invoice prefix')}
        <Field label="Number digits"><input className="input" type="number" min={1} max={8} value={s.numberPadding} onChange={e => set('numberPadding', Number(e.target.value))} /></Field>
        <label className="flex items-center gap-2 sm:col-span-2"><input type="checkbox" className="h-5 w-5" checked={s.includeFinancialYear} onChange={e => set('includeFinancialYear', e.target.checked)} /> Include financial year (April–March) and restart numbering each year</label>
        <p className="text-sm text-slate-600 sm:col-span-2">Next invoice number will look like: <b>{example}</b>. Numbers are given when a bill is finalized and never change.</p>
      </section>
    </div>
  )
}
