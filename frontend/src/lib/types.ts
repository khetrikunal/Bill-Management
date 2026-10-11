export type Role = 'ADMIN' | 'BILLING' | 'VIEWER'
export type BillStatus = 'DRAFT' | 'FINAL' | 'CANCELLED'
export type DocType = 'TAX_INVOICE' | 'MULTI_PO_INVOICE'

export interface User { id: number; username: string; fullName: string; role: Role; active: boolean }

export interface Item {
  id?: number | null
  poNumber: string
  name: string
  itemCode: string
  hsn: string
  qty: string          // kept as text while typing; validated before save
  unit: string
  rate: string
  cgstRate: string
  sgstRate: string
  taxable?: number; cgst?: number; sgst?: number; total?: number
}

export interface Seller {
  companyName: string; address: string; phone: string; email: string; gstin: string
  bankName: string; bankAccount: string; bankBranch: string; ifsc: string
  declaration: string; signatureText: string; footerNote: string; logo: string | null; pageSize: string
}

export interface Bill {
  id: number; docType: DocType; docTypeLabel: string; status: BillStatus
  invoiceNumber: string | null; displayNumber: string; invoiceDate: string
  transactionRef: string | null; poNumber: string | null
  buyerId: number | null; buyerName: string; buyerAddress: string; buyerGstin: string
  currency: string; notes: string | null; seller: Seller
  items: Array<Omit<Item, 'qty' | 'rate' | 'cgstRate' | 'sgstRate'> & { qty: number; rate: number; cgstRate: number; sgstRate: number }>
  totalTaxable: number; totalCgst: number; totalSgst: number; totalTax: number; grandTotal: number
  amountInWords: string; createdBy: string; createdAt: string; updatedAt: string
  finalizedAt: string | null; cancelledAt: string | null; cancelReason: string | null
  duplicatedFrom: number | null; version: number
}

export interface BillSummary {
  id: number; docType: DocType; docTypeLabel: string; status: BillStatus; invoiceNumber: string | null
  displayNumber: string; buyerName: string; invoiceDate: string; currency: string; grandTotal: number; createdAt: string
}

export interface Settings extends Omit<Seller, 'logo' | 'pageSize'> {
  logo: string | null; pageSize: string; defaultCurrency: string; defaultCgstRate: number; defaultSgstRate: number
  invoicePrefix: string; numberPadding: number; includeFinancialYear: boolean
}

export interface Customer { id?: number; name: string; address: string; gstin: string; email: string; phone: string }
export interface Product { id?: number; name: string; itemCode: string; hsn: string; unit: string; rate: number; cgstRate: number; sgstRate: number; active: boolean }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
