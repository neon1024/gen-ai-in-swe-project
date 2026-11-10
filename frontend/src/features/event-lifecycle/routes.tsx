import type { RouteObject } from 'react-router'
import { EventLifecyclePage } from './lifecycle/EventLifecyclePage'
import { EventSearchPage } from './search/EventSearchPage'

export const eventLifecycleRoutes: RouteObject[] = [
  { path: '/events', element: <EventSearchPage /> },
  { path: '/events/:eventId/lifecycle', element: <EventLifecyclePage /> },
]
