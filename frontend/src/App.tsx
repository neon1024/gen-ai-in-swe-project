import { BrowserRouter, useRoutes } from 'react-router'
import { eventAuthoringRoutes } from './features/event-authoring/routes'
import { eventLifecycleRoutes } from './features/event-lifecycle/routes'
import { authenticationRoutes } from './features/authentication/routes'
import { organizationMembershipRoutes } from './features/organization-membership/routes'
import { Toaster } from 'sonner'
import { AuthProvider } from './features/authentication/context'
import { RequireAuth } from './features/authentication/guards'
import { AppShell } from './components/AppShell'

function AppRoutes() {
  return useRoutes([
    ...authenticationRoutes,
    {
      element: <RequireAuth />,
      children: [
        ...eventLifecycleRoutes,
        { element: <AppShell />, children: [...organizationMembershipRoutes, ...eventAuthoringRoutes] },
      ],
    },
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
