import type { RouteObject } from 'react-router'
import AuthPage from './pages/AuthPage.tsx'
import { PublicOnly } from './guards'

export const authenticationRoutes: RouteObject[] = [
  {
    element: <PublicOnly />,
    children: [
      { path: '/login', element: <AuthPage /> },
      { path: '/signup', element: <AuthPage /> },
      { path: '/auth/login', element: <AuthPage /> },
      { path: '/auth/register', element: <AuthPage /> },
    ],
  },
]
