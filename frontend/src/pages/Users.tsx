import { useEffect, useState } from 'react'
import { Alert, Field, PageHeader, Spinner } from '../components/ui'
import { api, ApiError } from '../lib/api'
import type { Role, User } from '../lib/types'

export default function Users() {
  const [list, setList] = useState<User[] | null>(null)
  const [err, setErr] = useState('')
  const [fe, setFe] = useState<Record<string, string>>({})
  const [nu, setNu] = useState({ username: '', fullName: '', role: 'BILLING' as Role, password: '' })
  const [show, setShow] = useState(false)
  const load = () => api.get<User[]>('/users').then(setList).catch(e => setErr(e.message))
  useEffect(() => { load() }, [])

  async function create() {
    setErr(''); setFe({})
    try { await api.post('/users', nu); setNu({ username: '', fullName: '', role: 'BILLING', password: '' }); setShow(false); load() }
    catch (e) { setErr((e as ApiError).message); setFe((e as ApiError).fieldErrors) }
  }
  async function patch(u: User, body: object) {
    setErr('')
    try { await api.put(`/users/${u.id}`, body); load() } catch (e) { setErr((e as Error).message) }
  }

  return (
    <div>
      <PageHeader title="Users"><button className="btn btn-primary" onClick={() => setShow(s => !s)}>+ Add user</button></PageHeader>
      {err && <Alert>{err}</Alert>}
      {show && (
        <div className="card my-3 grid gap-3 sm:grid-cols-2">
          <Field label="Username" error={fe.username}><input className="input" value={nu.username} onChange={e => setNu({ ...nu, username: e.target.value })} /></Field>
          <Field label="Full name" error={fe.fullName}><input className="input" value={nu.fullName} onChange={e => setNu({ ...nu, fullName: e.target.value })} /></Field>
          <Field label="Role"><select className="input" value={nu.role} onChange={e => setNu({ ...nu, role: e.target.value as Role })}><option value="ADMIN">Admin</option><option value="BILLING">Billing staff</option><option value="VIEWER">Viewer</option></select></Field>
          <Field label="Password (min 8 characters)" error={fe.password}><input type="password" className="input" value={nu.password} onChange={e => setNu({ ...nu, password: e.target.value })} /></Field>
          <div className="sm:col-span-2"><button className="btn btn-primary" onClick={create}>Create user</button></div>
        </div>
      )}
      {!list ? <Spinner /> : (
        <div className="card mt-3 divide-y p-0 sm:p-0">
          {list.map(u => (
            <div key={u.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
              <div><div className="font-semibold">{u.fullName} <span className="font-normal text-slate-500">({u.username})</span></div>{!u.active && <span className="text-xs text-red-600">Deactivated</span>}</div>
              <div className="flex flex-wrap items-center gap-2">
                <select className="input !w-auto !py-2" value={u.role} onChange={e => patch(u, { role: e.target.value })}><option value="ADMIN">Admin</option><option value="BILLING">Billing staff</option><option value="VIEWER">Viewer</option></select>
                <button className="btn" onClick={() => { const p = prompt('New password (min 8 characters):'); if (p) patch(u, { password: p }) }}>Reset password</button>
                <button className="btn" onClick={() => patch(u, { active: !u.active })}>{u.active ? 'Deactivate' : 'Activate'}</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
