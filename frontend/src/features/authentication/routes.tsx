import { Navigate, type RouteObject } from 'react-router'
import AuthPage from './pages/AuthPage.tsx'
import { PublicOnly } from './guards'
import VerifyAccountPage from "./pages/VerifyAccountPage.tsx";
import CheckEmailPage from "./pages/CheckEmailPage.tsx";

export const authenticationRoutes: RouteObject[] = [
  {
    element: <PublicOnly />,
    children: [
      { path: '/login', element: <AuthPage /> },
      { path: '/signup', element: <AuthPage /> },
      { path: '/verify-email', element: <VerifyAccountPage /> },
      { path: '/check-email', element: <CheckEmailPage/> },
      { path: '/auth/login', element: <Navigate to="/login" replace /> },
      { path: '/auth/register', element: <Navigate to="/signup" replace /> },
    ],
  },
]
