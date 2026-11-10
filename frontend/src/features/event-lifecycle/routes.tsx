import type { RouteObject } from 'react-router'
import { GiveFeedbackPage } from './feedback/GiveFeedbackPage'
import { EventLifecyclePage } from './lifecycle/EventLifecyclePage'
import { EventReportPage } from './reports/EventReportPage'
import { EventSearchPage } from './search/EventSearchPage'

export const eventLifecycleRoutes: RouteObject[] = [
  { path: '/events', element: <EventSearchPage /> },
  { path: '/events/:eventId/lifecycle', element: <EventLifecyclePage /> },
  { path: '/events/:eventId/report', element: <EventReportPage /> },
  { path: '/activities/:activityId/feedback', element: <GiveFeedbackPage /> },
]
