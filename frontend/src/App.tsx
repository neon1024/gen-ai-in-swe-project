import { BrowserRouter, useRoutes } from 'react-router'
import { eventLifecycleRoutes } from './features/event-lifecycle/routes'
import { organizationMembershipRoutes } from './features/organization-membership/routes'

function AppRoutes() {
  return useRoutes([...eventLifecycleRoutes, ...organizationMembershipRoutes])
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}
