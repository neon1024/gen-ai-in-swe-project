import type { RouteObject } from 'react-router'
import { EventAuthoringPage } from './EventAuthoringPage'
import { NewEventPage } from './NewEventPage'

export const eventAuthoringRoutes: RouteObject[] = [
  { path: '/authoring/events/new', element: <NewEventPage /> },
  { path: '/authoring/events/:eventId', element: <EventAuthoringPage /> },
]
