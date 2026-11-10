import type { RouteObject } from 'react-router'
import { EventSearchPage } from './search/EventSearchPage'

export const eventLifecycleRoutes: RouteObject[] = [{ path: '/events', element: <EventSearchPage /> }]
