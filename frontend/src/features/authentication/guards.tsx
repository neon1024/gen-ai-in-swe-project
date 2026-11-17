import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './context'

export function RequireAuth() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  return isAuthenticated
    ? <Outlet />
    : <Navigate to="/login" replace state={{ from: location.pathname }} />
}

export function PublicOnly() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Navigate to="/events" replace /> : <Outlet />
}
