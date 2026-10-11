import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Alert } from '../components/ui'
import { useAuth } from '../lib/auth'

export default function Login() {
  const { user, login } = useAuth()
  const nav = useNavigate()
  const [u, setU] = useState('')
  const [p, setP] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  if (user) return <Navigate to="/" replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setErr('')
    try { await login(u.trim(), p); nav('/') } catch (x) { setErr((x as Error).message) } finally { setBusy(false) }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <form onSubmit={submit} className="card w-full max-w-sm space-y-4">
        <h1 className="text-2xl font-bold">Bill Manager</h1>
        <p className="text-sm text-slate-600">Sign in to create and manage bills.</p>
        {err && <Alert>{err}</Alert>}
        <div><label className="label" htmlFor="u">Username</label><input id="u" className="input" autoComplete="username" value={u} onChange={e => setU(e.target.value)} required autoFocus /></div>
        <div><label className="label" htmlFor="p">Password</label><input id="p" type="password" className="input" autoComplete="current-password" value={p} onChange={e => setP(e.target.value)} required /></div>
        <button className="btn btn-primary w-full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
    </div>
  )
}
