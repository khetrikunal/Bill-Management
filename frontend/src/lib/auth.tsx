import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, getToken, setToken, setUnauthorizedHandler } from './api'
import type { Role, User } from './types'

interface AuthCtx {
  user: User | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  can: (...roles: Role[]) => boolean
}
const Ctx = createContext<AuthCtx>(null as never)
export const useAuth = () => useContext(Ctx)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(!!getToken())

  const logout = useCallback(() => { setToken(null); setUser(null) }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!getToken()) return
    api.get<User>('/auth/me').then(setUser).catch(logout).finally(() => setLoading(false))
  }, [logout])

  const login = useCallback(async (username: string, password: string) => {
    const r = await api.post<{ token: string; user: User }>('/auth/login', { username, password })
    setToken(r.token)
    setUser(r.user)
  }, [])

  const value = useMemo<AuthCtx>(() => ({
    user, loading, login, logout,
    can: (...roles) => !!user && roles.includes(user.role),
  }), [user, loading, login, logout])

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}
