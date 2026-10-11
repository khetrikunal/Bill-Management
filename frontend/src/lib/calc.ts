// Client-side mirror of the server's calculation, used only for the live preview while typing.
// The server recalculates everything on save, so the saved document is always authoritative.

export function round2(n: number): number {
  if (!Number.isFinite(n)) return 0
  const sign = n < 0 ? -1 : 1
  return (sign * Math.round(Math.abs(n) * 100 + 1e-9)) / 100
}

export interface LineIn { qty: number; rate: number; cgstRate: number; sgstRate: number }
export interface LineOut { taxable: number; cgst: number; sgst: number; total: number }

/** Taxable = qty x rate; CGST/SGST = taxable x rate% / 100; Total = taxable + CGST + SGST (same as the workbook). */
export function calcLine(i: LineIn): LineOut {
  const taxable = round2(i.qty * i.rate)
  const cgst = round2((taxable * i.cgstRate) / 100)
  const sgst = round2((taxable * i.sgstRate) / 100)
  return { taxable, cgst, sgst, total: round2(taxable + cgst + sgst) }
}

export interface Totals { taxable: number; cgst: number; sgst: number; tax: number; grand: number }

export function calcTotals(lines: LineOut[]): Totals {
  const sum = (k: keyof LineOut) => round2(lines.reduce((a, l) => a + l[k], 0))
  const taxable = sum('taxable'), cgst = sum('cgst'), sgst = sum('sgst')
  const tax = round2(cgst + sgst)
  return { taxable, cgst, sgst, tax, grand: round2(taxable + tax) }
}

const ONES = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten', 'Eleven', 'Twelve',
  'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen']
const TENS = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety']

const below100 = (n: number) => (n < 20 ? ONES[n] : TENS[Math.floor(n / 10)] + (n % 10 ? ' ' + ONES[n % 10] : ''))
const below1000 = (n: number) => {
  const h = Math.floor(n / 100), r = n % 100
  return [h ? ONES[h] + ' Hundred' : '', r ? below100(r) : ''].filter(Boolean).join(' ')
}
function indian(n: number): string {
  const parts: string[] = []
  const crore = Math.floor(n / 1e7); n %= 1e7
  const lakh = Math.floor(n / 1e5); n %= 1e5
  const thou = Math.floor(n / 1e3); n %= 1e3
  if (crore) parts.push(indian(crore) + ' Crore')
  if (lakh) parts.push(below100(lakh) + ' Lakh')
  if (thou) parts.push(below100(thou) + ' Thousand')
  if (n) parts.push(below1000(n))
  return parts.join(' ')
}
function international(n: number): string {
  const parts: string[] = []
  for (const [d, name] of [[1e12, 'Trillion'], [1e9, 'Billion'], [1e6, 'Million'], [1e3, 'Thousand']] as const) {
    const p = Math.floor(n / d)
    if (p) { parts.push(below1000(p) + ' ' + name); n %= d }
  }
  if (n) parts.push(below1000(n))
  return parts.join(' ')
}

export function amountInWords(amount: number, currency: string): string {
  const cents = Math.round(Math.abs(amount) * 100)
  const whole = Math.floor(cents / 100), minor = cents % 100
  const inr = currency === 'INR'
  let s = `${currency} ${whole === 0 ? 'Zero' : inr ? indian(whole) : international(whole)}`
  if (minor) s += ` and ${below100(minor)} ${inr ? 'Paise' : 'Cents'}`
  return s + ' Only'
}

export function money(n: number, currency = 'INR'): string {
  return new Intl.NumberFormat(currency === 'INR' ? 'en-IN' : 'en-US', {
    minimumFractionDigits: 2, maximumFractionDigits: 2,
  }).format(n)
}

export function pct(n: number): string {
  return (Number.isInteger(n) ? n.toFixed(1) : String(n)) + '%'
}
