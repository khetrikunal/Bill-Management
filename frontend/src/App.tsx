import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import { Spinner } from './components/ui'
import { useAuth } from './lib/auth'
import type { Role } from './lib/types'
import BillForm from './pages/BillForm'
import BillList from './pages/BillList'
import BillView from './pages/BillView'
import Customers from './pages/Customers'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import Products from './pages/Products'
import SettingsPage from './pages/SettingsPage'
import Users from './pages/Users'
import { CompanyProvider } from './lib/company'

function Guard({ roles, children }: { roles?: Role[]; children: JSX.Element }) {
  const { user, loading } = useAuth()
  if (loading) return <Spinner />
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Guard><CompanyProvider><Layout /></CompanyProvider></Guard>}>
        <Route index element={<Dashboard />} />
        <Route path="bills" element={<BillList />} />
        <Route path="bills/new" element={<Guard roles={['ADMIN', 'BILLING']}><BillForm /></Guard>} />
        <Route path="bills/:id" element={<BillView />} />
        <Route path="bills/:id/edit" element={<Guard roles={['ADMIN', 'BILLING']}><BillForm /></Guard>} />
        <Route path="customers" element={<Customers />} />
        <Route path="products" element={<Products />} />
        <Route path="settings" element={<Guard roles={['ADMIN']}><SettingsPage /></Guard>} />
        <Route path="users" element={<Guard roles={['ADMIN']}><Users /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
