import { BrowserRouter, useRoutes } from 'react-router'
import { eventLifecycleRoutes } from './features/event-lifecycle/routes'
import {authenticationRoutes} from "./features/authentication/routes.tsx";

function AppRoutes() {
  return useRoutes([...eventLifecycleRoutes, ...authenticationRoutes]);
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}
