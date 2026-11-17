import { BrowserRouter, useRoutes } from 'react-router'
import { eventLifecycleRoutes } from './features/event-lifecycle/routes'
import { authenticationRoutes } from './features/authentication/routes'
import { Toaster } from 'sonner'
import { AuthProvider } from './features/authentication/context'
import { RequireAuth } from './features/authentication/guards'

function AppRoutes() {
  return useRoutes([
    ...authenticationRoutes,
    { element: <RequireAuth />, children: eventLifecycleRoutes },
  ])
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
        <Toaster position="bottom-right" richColors />
      </AuthProvider>
    </BrowserRouter>
  )
}
