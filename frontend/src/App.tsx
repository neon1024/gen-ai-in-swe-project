import { BrowserRouter, useRoutes } from 'react-router'
import { eventLifecycleRoutes } from './features/event-lifecycle/routes'
import { Toaster } from 'sonner'

function AppRoutes() {
  return useRoutes(eventLifecycleRoutes)
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
      <Toaster position="bottom-right" richColors />
    </BrowserRouter>
  )
}
