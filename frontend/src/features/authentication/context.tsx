import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import type { LoginResponse } from './api'
import { logout as logoutRequest } from './api'

const SESSION_KEY = 'ubb.events.session'

type AuthContextValue = {
  user: LoginResponse | null
  isAuthenticated: boolean
  setUser: (user: LoginResponse) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  isAuthenticated: false,
  setUser: () => undefined,
  logout: () => undefined,
})

function readSession(): LoginResponse | null {
  try {
    const stored = localStorage.getItem(SESSION_KEY)
    return stored ? JSON.parse(stored) as LoginResponse : null
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<LoginResponse | null>(readSession)
  const navigate = useNavigate()

  const setUser = useCallback((nextUser: LoginResponse) => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(nextUser))
    localStorage.setItem('devUserId', nextUser.userId)
    setUserState(nextUser)
  }, [])

  const logout = useCallback(() => {
    void logoutRequest().catch(() => undefined)
    localStorage.removeItem(SESSION_KEY)
    localStorage.removeItem('devUserId')
    setUserState(null)
    navigate('/login', { replace: true })
  }, [navigate])

  useEffect(() => {
    const handleUnauthorized = () => {
      localStorage.removeItem(SESSION_KEY)
      localStorage.removeItem('devUserId')
      setUserState(null)
      navigate('/login', { replace: true })
    }
    window.addEventListener('auth:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized)
  }, [navigate])

  const value = useMemo(() => ({
    user,
    isAuthenticated: user !== null,
    setUser,
    logout,
  }), [logout, setUser, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
