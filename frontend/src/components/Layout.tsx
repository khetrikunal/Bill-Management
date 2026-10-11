import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth'

export default function Layout() {
  const { user, logout, can } = useAuth()
  const nav = useNavigate()
  const [open, setOpen] = useState(false)

  const links: Array<[string, string, boolean]> = [
    ['/', 'Dashboard', true],
    ['/bills/new', 'Create Bill', can('ADMIN', 'BILLING')],
    ['/bills', 'Bill Records', true],
    ['/customers', 'Buyers', true],
    ['/products', 'Products', true],
    ['/settings', 'Company Settings', can('ADMIN')],
    ['/users', 'Users', can('ADMIN')],
  ]
  const cls = ({ isActive }: { isActive: boolean }) =>
    `block rounded-lg px-3 py-3 text-sm font-medium lg:py-2 ${isActive ? 'bg-white/15 text-white' : 'text-slate-200 hover:bg-white/10'}`

  return (
    <div className="min-h-screen lg:flex">
      <header className="no-print sticky top-0 z-30 flex items-center justify-between bg-navy-900 px-4 py-3 text-white lg:hidden">
        <span className="font-bold">Bill Manager</span>
        <button className="rounded-lg px-3 py-2 text-xl" aria-label="Menu" onClick={() => setOpen(o => !o)}>{open ? '✕' : '☰'}</button>
      </header>
      <aside className={`no-print ${open ? 'block' : 'hidden'} bg-navy-900 p-3 text-white lg:sticky lg:top-0 lg:block lg:h-screen lg:w-60 lg:shrink-0`}>
        <div className="mb-4 hidden px-3 pt-2 text-lg font-bold lg:block">Bill Manager</div>
        <nav className="space-y-1" onClick={() => setOpen(false)}>
          {links.filter(l => l[2]).map(([to, label]) => <NavLink key={to} to={to} end={to === '/' || to === '/bills'} className={cls}>{label}</NavLink>)}
        </nav>
        <div className="mt-6 border-t border-white/20 px-3 pt-3 text-sm">
          <div className="font-semibold">{user?.fullName}</div>
          <div className="text-slate-300">{user?.role}</div>
          <button className="mt-2 rounded-lg bg-white/10 px-3 py-2 text-sm hover:bg-white/20" onClick={() => { logout(); nav('/login') }}>Log out</button>
        </div>
      </aside>
      <main className="min-w-0 flex-1 p-3 sm:p-6"><Outlet /></main>
    </div>
  )
}
