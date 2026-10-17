import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from './api'
import type { CompanyOption } from './types'

interface CompanyContextValue {
  companies: CompanyOption[]
  companyId: number
  selectCompany: (id: number) => void
  error: string
}

const CompanyContext = createContext<CompanyContextValue | null>(null)

export function CompanyProvider({ children }: { children: ReactNode }) {
  const [companies, setCompanies] = useState<CompanyOption[]>([])
  const [companyId, setCompanyId] = useState(() => {
    const saved = Number(localStorage.getItem('bm_company_id') || '1')
    return Number.isInteger(saved) && saved > 0 ? saved : 1
  })
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<CompanyOption[]>('/settings/companies').then(options => {
      setCompanies(options)
      if (!options.some(option => option.id === companyId)) {
        const first = options[0]?.id ?? 1
        setCompanyId(first)
        localStorage.setItem('bm_company_id', String(first))
      }
    }).catch(e => setError(e.message))
  }, [])

  const selectCompany = useCallback((id: number) => {
    if (!companies.some(option => option.id === id)) return
    setCompanyId(id)
    localStorage.setItem('bm_company_id', String(id))
  }, [companies])

  const value = useMemo(() => ({ companies, companyId, selectCompany, error }), [companies, companyId, error])
  return <CompanyContext.Provider value={value}>{children}</CompanyContext.Provider>
}

export function useCompany() {
  const context = useContext(CompanyContext)
  if (!context) throw new Error('useCompany must be used inside CompanyProvider')
  return context
}
