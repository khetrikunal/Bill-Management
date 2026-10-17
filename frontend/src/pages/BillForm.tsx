import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import InvoicePreview, { type PreviewData } from '../components/InvoicePreview'
import { Alert, Field, PageHeader, Spinner } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { amountInWords, calcLine, calcTotals, money } from '../lib/calc'
import type { Bill, Customer, DocType, Item, Product, Settings } from '../lib/types'
import { useCompany } from '../lib/company'

const TYPES: Array<{ code: DocType; title: string; text: string }> = [
  { code: 'TAX_INVOICE', title: 'Tax Invoice (single P.O.)', text: 'One P.O. number for the whole bill. Matches Sheet1 of your Excel file.' },
  { code: 'MULTI_PO_INVOICE', title: 'Tax Invoice (multiple P.O.s)', text: 'Items grouped under different P.O. numbers. Matches Sheet2 of your Excel file.' },
]
const today = () => new Date().toISOString().slice(0, 10)
const toNum = (s: string) => (s.trim() === '' ? NaN : Number(s))

export default function BillForm() {
  const { id } = useParams()
  const [sp] = useSearchParams()
  const nav = useNavigate()
  const editing = !!id
  const { companies, companyId, selectCompany } = useCompany()

  const [settings, setSettings] = useState<Settings | null>(null)
  const [customers, setCustomers] = useState<Customer[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState<'edit' | 'preview'>('edit')
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState('')
  const [fieldErr, setFieldErr] = useState<Record<string, string>>({})

  const [docType, setDocType] = useState<DocType>((sp.get('type') as DocType) === 'MULTI_PO_INVOICE' ? 'MULTI_PO_INVOICE' : 'TAX_INVOICE')
  const [invoiceDate, setInvoiceDate] = useState(today())
  const [dueDate, setDueDate] = useState('')
  const [paymentTerms, setPaymentTerms] = useState('')
  const [poNumber, setPoNumber] = useState('')
  const [transactionRef, setTransactionRef] = useState('')
  const [buyerId, setBuyerId] = useState<number | null>(null)
  const [buyerName, setBuyerName] = useState('')
  const [buyerAddress, setBuyerAddress] = useState('')
  const [buyerGstin, setBuyerGstin] = useState('')
  const [buyerPhone, setBuyerPhone] = useState('')
  const [buyerEmail, setBuyerEmail] = useState('')
  const [shippingAddress, setShippingAddress] = useState('')
  const [currency, setCurrency] = useState('INR')
  const [notes, setNotes] = useState('')
  const [version, setVersion] = useState<number | null>(null)
  const [remember, setRemember] = useState(false)
  const [items, setItems] = useState<Item[]>([])

  const blankItem = (s: Settings, po = ''): Item => ({
    poNumber: po, name: '', itemCode: '', hsn: '', qty: '', unit: 'nos', rate: '',
    cgstRate: String(s.defaultCgstRate), sgstRate: String(s.defaultSgstRate),
  })

  useEffect(() => {
    const invoiceCompanyId = Number(sp.get('companyId'))
    if (editing && invoiceCompanyId && invoiceCompanyId !== companyId) {
      selectCompany(invoiceCompanyId)
      return
    }
    let active = true
    setLoading(true)
    setSettings(null);
    (async () => {
      try {
        const [s, c, p] = await Promise.all([
          api.get<Settings>('/settings'), api.get<Customer[]>('/customers'), api.get<Product[]>('/products'),
        ])
        if (!active) return
        setSettings(s); setCustomers(c); setProducts(p.filter(x => x.active))
        if (id) {
          const b = await api.get<Bill>(`/bills/${id}`)
          if (!active) return
          if (b.companyId !== companyId) {
            selectCompany(b.companyId)
            return
          }
          if (b.status !== 'DRAFT') { nav(`/bills/${id}`, { replace: true }); return }
          setDocType(b.docType); setInvoiceDate(b.invoiceDate); setPoNumber(b.poNumber ?? '')
          setDueDate(b.dueDate ?? ''); setPaymentTerms(b.paymentTerms ?? '')
          setTransactionRef(b.transactionRef ?? ''); setBuyerId(b.buyerId); setBuyerName(b.buyerName)
          setBuyerAddress(b.buyerAddress); setBuyerGstin(b.buyerGstin); setBuyerPhone(b.buyerPhone)
          setBuyerEmail(b.buyerEmail); setShippingAddress(b.shippingAddress); setCurrency(b.currency)
          setNotes(b.notes ?? ''); setVersion(b.version)
          setItems(b.items.map(i => ({
            poNumber: i.poNumber ?? '', name: i.name, itemCode: i.itemCode ?? '', hsn: i.hsn ?? '', qty: String(i.qty),
            unit: i.unit, rate: String(i.rate), cgstRate: String(i.cgstRate), sgstRate: String(i.sgstRate),
          })))
        } else {
          setInvoiceDate(today()); setPoNumber(''); setTransactionRef('')
          setDueDate(''); setPaymentTerms(s.defaultPaymentTerms || '')
          setBuyerId(null); setBuyerName(''); setBuyerAddress(''); setBuyerGstin('')
          setBuyerPhone(''); setBuyerEmail(''); setShippingAddress('')
          setNotes(''); setVersion(null)
          setCurrency(s.defaultCurrency)
          setItems([blankItem(s)])
        }
      } catch (e) { if (active) setErr((e as Error).message) } finally { if (active) setLoading(false) }
    })()
    // eslint-disable-next-line react-hooks/exhaustive-deps
    return () => { active = false }
  }, [id, companyId])

  // ---- live calculation (preview only; the server recalculates on save)
  const calc = useMemo(() => {
    const lines = items.map(i => {
      const v = { qty: toNum(i.qty), rate: toNum(i.rate), cgstRate: toNum(i.cgstRate), sgstRate: toNum(i.sgstRate) }
      return calcLine({ qty: v.qty || 0, rate: v.rate || 0, cgstRate: v.cgstRate || 0, sgstRate: v.sgstRate || 0 })
    })
    const totals = calcTotals(lines)
    return { lines, totals }
  }, [items])

  function setItem(idx: number, patch: Partial<Item>) {
    setItems(list => list.map((it, i) => (i === idx ? { ...it, ...patch } : it)))
  }

  function pickProduct(idx: number, name: string) {
    const p = products.find(x => x.name.toLowerCase() === name.trim().toLowerCase())
    if (p) setItem(idx, { name: p.name, itemCode: p.itemCode, hsn: p.hsn, unit: p.unit, rate: String(p.rate), cgstRate: String(p.cgstRate), sgstRate: String(p.sgstRate) })
    else setItem(idx, { name })
  }

  function pickCustomer(v: string) {
    if (!v) { setBuyerId(null); return }
    const c = customers.find(x => String(x.id) === v)
    if (c) {
      setBuyerId(c.id ?? null); setBuyerName(c.name); setBuyerAddress(c.address); setBuyerGstin(c.gstin)
      setBuyerPhone(c.phone); setBuyerEmail(c.email)
    }
  }

  function validate(): Record<string, string> {
    const e: Record<string, string> = {}
    if (!invoiceDate) e.invoiceDate = 'Required'
    if (!buyerName.trim()) e.buyerName = 'Required'
    if (!buyerAddress.trim()) e.buyerAddress = 'Required'
    if (buyerGstin && !/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/.test(buyerGstin)) e.buyerGstin = 'Invalid GSTIN format'
    if (docType === 'TAX_INVOICE' && !poNumber.trim()) e.poNumber = 'P.O. number is required'
    if (items.length === 0) e.items = 'Add at least one item'
    items.forEach((i, n) => {
      if (!i.name.trim()) e[`i${n}.name`] = 'Required'
      if (!(toNum(i.qty) > 0)) e[`i${n}.qty`] = 'Enter a quantity above 0'
      if (!(toNum(i.rate) >= 0)) e[`i${n}.rate`] = 'Enter a rate'
      if (!i.unit.trim()) e[`i${n}.unit`] = 'Required'
      if (i.hsn && !/^\d{4,8}$/.test(i.hsn)) e[`i${n}.hsn`] = '4 to 8 digits'
      for (const k of ['cgstRate', 'sgstRate'] as const) {
        const v = toNum(i[k]); if (!(v >= 0 && v <= 100)) e[`i${n}.${k}`] = '0 to 100'
      }
      if (docType === 'MULTI_PO_INVOICE' && !i.poNumber.trim()) e[`i${n}.poNumber`] = 'P.O. required'
    })
    return e
  }

  async function save() {
    setErr('')
    const e = validate()
    setFieldErr(e)
    if (Object.keys(e).length) { setErr('Please fix the highlighted fields.'); setTab('edit'); window.scrollTo({ top: 0, behavior: 'smooth' }); return }
    setSaving(true)
    try {
      let bid = buyerId
      if (remember && !bid) {
        const c = await api.post<Customer>('/customers', { name: buyerName.trim(), address: buyerAddress.trim(), gstin: buyerGstin, email: '', phone: '' })
        bid = c.id ?? null
      }
      const body = {
        docType, invoiceDate, dueDate: dueDate || null, paymentTerms, transactionRef: transactionRef || null,
        poNumber: docType === 'TAX_INVOICE' ? poNumber : null,
        buyerId: bid, buyerName, buyerAddress, buyerGstin, buyerPhone, buyerEmail, shippingAddress,
        currency, notes: notes || null, version,
        items: items.map(i => ({
          poNumber: docType === 'MULTI_PO_INVOICE' ? i.poNumber : null, name: i.name, itemCode: i.itemCode || null, hsn: i.hsn || null,
          qty: toNum(i.qty), unit: i.unit, rate: toNum(i.rate), cgstRate: toNum(i.cgstRate), sgstRate: toNum(i.sgstRate),
        })),
      }
      const saved = editing ? await api.put<Bill>(`/bills/${id}`, body) : await api.post<Bill>('/bills', body)
      nav(`/bills/${saved.id}`)
    } catch (x) {
      const a = x as ApiError
      setErr(a.message)
      const mapped: Record<string, string> = {}
      Object.entries(a.fieldErrors || {}).forEach(([k, v]) => { mapped[k.replace(/^items\[(\d+)\]\./, 'i$1.')] = v })
      setFieldErr(mapped)
    } finally { setSaving(false) }
  }

  if (loading || !settings) return err ? <Alert>{err}</Alert> : <Spinner />

  const preview: PreviewData = {
    docType, number: 'DRAFT', invoiceDate, poNumber, transactionRef: transactionRef || null,
    buyerName, buyerAddress, buyerGstin, buyerPhone, buyerEmail, shippingAddress,
    dueDate, paymentTerms, currency,
    seller: { ...settings, logo: settings.logo, pageSize: settings.pageSize },
    items: items.map((i, n) => ({
      poNumber: i.poNumber, name: i.name, itemCode: i.itemCode, hsn: i.hsn, qty: toNum(i.qty), unit: i.unit,
      rate: toNum(i.rate) || 0, cgstRate: toNum(i.cgstRate) || 0, sgstRate: toNum(i.sgstRate) || 0, ...calc.lines[n],
    })),
    totals: calc.totals, amountInWords: amountInWords(calc.totals.grand, currency), status: 'DRAFT',
  }

  const E = (k: string) => fieldErr[k]
  const multi = docType === 'MULTI_PO_INVOICE'

  return (
    <div>
      <PageHeader title={editing ? 'Edit draft bill' : 'Create bill'}>
        <button className={`btn ${tab === 'edit' ? 'btn-primary' : ''}`} onClick={() => setTab('edit')}>Edit</button>
        <button className={`btn ${tab === 'preview' ? 'btn-primary' : ''}`} onClick={() => setTab('preview')}>Preview</button>
      </PageHeader>
      {err && <div className="mb-3"><Alert>{err}</Alert></div>}

      {tab === 'preview' ? (
        <div>
          <p className="no-print mb-2 text-sm text-slate-600">This is how the bill will look. Totals are recalculated by the server when you save.</p>
          <InvoicePreview data={preview} />
        </div>
      ) : (
        <div className="space-y-4">
          {!editing && (
            <>
            <section className="card">
              <Field label="Select Business / Company">
                <select className="input" value={companyId} onChange={e => selectCompany(Number(e.target.value))}>
                  {companies.map(company => <option key={company.id} value={company.id}>{company.displayName}</option>)}
                </select>
              </Field>
              <p className="mt-2 text-sm text-slate-600">Invoices and numbering are kept separate for each business.</p>
            </section>
            <div className="grid gap-3 sm:grid-cols-2">
              {TYPES.map(t => (
                <button key={t.code} type="button" onClick={() => setDocType(t.code)}
                  className={`card text-left ${docType === t.code ? 'ring-2 ring-navy-700' : 'hover:bg-slate-50'}`}>
                  <div className="font-semibold">{t.title}</div><div className="text-sm text-slate-600">{t.text}</div>
                </button>
              ))}
            </div>
            </>
          )}
          {editing && <div className="rounded-lg bg-slate-100 px-4 py-3 text-sm font-semibold">Business: {settings.displayName || settings.companyName}</div>}

          <section className="card grid gap-4 sm:grid-cols-2">
            <h2 className="text-lg font-semibold sm:col-span-2">Bill details</h2>
            <Field label="Invoice date" error={E('invoiceDate')}><input type="date" className="input" value={invoiceDate} onChange={e => setInvoiceDate(e.target.value)} /></Field>
            <Field label="Due date"><input type="date" className="input" value={dueDate} onChange={e => setDueDate(e.target.value)} /></Field>
            <Field label="Invoice number"><input className="input" value="Assigned when you finalize" disabled /></Field>
            <Field label="Payment terms"><input className="input" value={paymentTerms} onChange={e => setPaymentTerms(e.target.value)} /></Field>
            {!multi && <Field label="P.O. number" error={E('poNumber')}><input className="input" inputMode="numeric" value={poNumber} onChange={e => setPoNumber(e.target.value)} /></Field>}
            <Field label="Reference / transaction no. (optional, links related bills)" error={E('transactionRef')}><input className="input" value={transactionRef} onChange={e => setTransactionRef(e.target.value)} /></Field>
            <Field label="Currency" error={E('currency')}>
              <select className="input" value={currency} onChange={e => setCurrency(e.target.value)}>
                {Array.from(new Set(['INR', 'USD', 'EUR', 'GBP', 'AED', settings.defaultCurrency, currency])).map(c => <option key={c}>{c}</option>)}
              </select>
            </Field>
          </section>

          <section className="card grid gap-4 sm:grid-cols-2">
            <h2 className="text-lg font-semibold sm:col-span-2">Buyer</h2>
            <Field label="Saved buyers" className="sm:col-span-2">
              <select className="input" value={buyerId ?? ''} onChange={e => pickCustomer(e.target.value)}>
                <option value="">— Enter manually —</option>
                {customers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
            <Field label="Buyer name" error={E('buyerName')}><input className="input" value={buyerName} onChange={e => { setBuyerName(e.target.value); setBuyerId(null) }} /></Field>
            <Field label="Buyer GSTIN" error={E('buyerGstin')}><input className="input uppercase" value={buyerGstin} onChange={e => setBuyerGstin(e.target.value.toUpperCase())} /></Field>
            <Field label="Buyer address" error={E('buyerAddress')} className="sm:col-span-2"><textarea className="input" rows={3} value={buyerAddress} onChange={e => setBuyerAddress(e.target.value)} /></Field>
            <Field label="Buyer phone"><input className="input" value={buyerPhone} onChange={e => setBuyerPhone(e.target.value)} /></Field>
            <Field label="Buyer email"><input className="input" type="email" value={buyerEmail} onChange={e => setBuyerEmail(e.target.value)} /></Field>
            <Field label="Shipping address (optional)" className="sm:col-span-2"><textarea className="input" rows={2} value={shippingAddress} onChange={e => setShippingAddress(e.target.value)} /></Field>
            {!buyerId && <label className="flex items-center gap-2 text-sm sm:col-span-2"><input type="checkbox" className="h-5 w-5" checked={remember} onChange={e => setRemember(e.target.checked)} /> Save this buyer for next time</label>}
          </section>

          <section className="space-y-3">
            <h2 className="text-lg font-semibold">Items</h2>
            {E('items') && <p className="err">{E('items')}</p>}
            <datalist id="products">{products.map(p => <option key={p.id} value={p.name} />)}</datalist>
            {items.map((it, n) => (
              <div key={n} className="card grid grid-cols-2 gap-3 sm:grid-cols-6">
                <div className="col-span-2 flex items-center justify-between sm:col-span-6">
                  <span className="font-semibold">Item {n + 1}</span>
                  {items.length > 1 && <button type="button" className="btn !min-h-[36px] text-red-700" onClick={() => setItems(l => l.filter((_, i) => i !== n))}>Remove</button>}
                </div>
                {multi && <Field label="P.O. number" error={E(`i${n}.poNumber`)} className="col-span-2 sm:col-span-2"><input className="input" inputMode="numeric" value={it.poNumber} onChange={e => setItem(n, { poNumber: e.target.value })} /></Field>}
                <Field label="Product name (type or pick)" error={E(`i${n}.name`)} className={`col-span-2 ${multi ? 'sm:col-span-4' : 'sm:col-span-4'}`}><input className="input" list="products" value={it.name} onChange={e => pickProduct(n, e.target.value)} /></Field>
                <Field label="Item code" className="col-span-2 sm:col-span-2"><input className="input" value={it.itemCode} onChange={e => setItem(n, { itemCode: e.target.value })} /></Field>
                <Field label="HSN/SAC" error={E(`i${n}.hsn`)}><input className="input" inputMode="numeric" value={it.hsn} onChange={e => setItem(n, { hsn: e.target.value.replace(/\D/g, '') })} /></Field>
                <Field label="Qty" error={E(`i${n}.qty`)}><input className="input" inputMode="decimal" value={it.qty} onChange={e => setItem(n, { qty: e.target.value })} /></Field>
                <Field label="Unit" error={E(`i${n}.unit`)}><input className="input" value={it.unit} onChange={e => setItem(n, { unit: e.target.value })} /></Field>
                <Field label="Rate" error={E(`i${n}.rate`)}><input className="input" inputMode="decimal" value={it.rate} onChange={e => setItem(n, { rate: e.target.value })} /></Field>
                <Field label="CGST %" error={E(`i${n}.cgstRate`)}><input className="input" inputMode="decimal" value={it.cgstRate} onChange={e => setItem(n, { cgstRate: e.target.value })} /></Field>
                <Field label="SGST %" error={E(`i${n}.sgstRate`)}><input className="input" inputMode="decimal" value={it.sgstRate} onChange={e => setItem(n, { sgstRate: e.target.value })} /></Field>
                <div className="col-span-2 flex flex-wrap justify-between gap-x-4 rounded-lg bg-slate-100 px-3 py-2 text-sm sm:col-span-6">
                  <span>Taxable {money(calc.lines[n].taxable, currency)}</span><span>CGST {money(calc.lines[n].cgst, currency)}</span>
                  <span>SGST {money(calc.lines[n].sgst, currency)}</span><b>Total {money(calc.lines[n].total, currency)}</b>
                </div>
              </div>
            ))}
            <button type="button" className="btn w-full sm:w-auto" onClick={() => setItems(l => [...l, blankItem(settings, multi ? l[l.length - 1]?.poNumber ?? '' : '')])}>+ Add item</button>
          </section>

          <section className="card">
            <Field label="Notes (internal, not printed)"><textarea className="input" rows={2} value={notes} onChange={e => setNotes(e.target.value)} /></Field>
          </section>

          <section className="card space-y-1">
            <div className="flex justify-between"><span>Total amount</span><span>{money(calc.totals.taxable, currency)}</span></div>
            <div className="flex justify-between"><span>Add CGST</span><span>{money(calc.totals.cgst, currency)}</span></div>
            <div className="flex justify-between"><span>Add SGST</span><span>{money(calc.totals.sgst, currency)}</span></div>
            <div className="flex justify-between"><span>Tax amount</span><span>{money(calc.totals.tax, currency)}</span></div>
            <div className="flex justify-between border-t pt-2 text-lg font-bold"><span>Total with tax</span><span>{currency} {money(calc.totals.grand, currency)}</span></div>
            <div className="text-sm text-slate-600">{amountInWords(calc.totals.grand, currency)}</div>
          </section>
        </div>
      )}

      <div className="no-print sticky bottom-0 -mx-3 mt-4 flex gap-2 border-t bg-white/95 p-3 backdrop-blur sm:-mx-6 sm:px-6">
        <button className="btn btn-primary flex-1 sm:flex-none" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Save draft'}</button>
        <button className="btn flex-1 sm:flex-none" onClick={() => setTab(tab === 'edit' ? 'preview' : 'edit')}>{tab === 'edit' ? 'Preview' : 'Back to edit'}</button>
        <button className="btn" onClick={() => nav(-1)}>Cancel</button>
      </div>
    </div>
  )
}
