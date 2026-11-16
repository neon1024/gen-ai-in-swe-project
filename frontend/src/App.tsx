import { BrowserRouter, useRoutes } from 'react-router'
import { eventAuthoringRoutes } from './features/event-authoring/routes'
import { eventLifecycleRoutes } from './features/event-lifecycle/routes'

function AppRoutes() {
  return useRoutes([...eventLifecycleRoutes, ...eventAuthoringRoutes])
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}
