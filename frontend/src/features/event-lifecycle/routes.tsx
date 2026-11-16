import type { RouteObject } from 'react-router'
import { GiveFeedbackPage } from './feedback/GiveFeedbackPage'
import { EventLifecyclePage } from './lifecycle/EventLifecyclePage'
import { EventReportPage } from './reports/EventReportPage'
import { EventSearchPage } from './search/EventSearchPage'
import { AppShell } from '../../components/AppShell'
import { AttendancePage } from './attendance/AttendancePage'
import { EventDetailsPage } from './participant/EventDetailsPage'
import { MyActivitiesPage } from './participant/MyActivitiesPage'
import { NotificationsPage } from './participant/NotificationsPage'
import { QrPassPage } from './participant/QrPassPage'

export const eventLifecycleRoutes: RouteObject[] = [
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <EventSearchPage /> },
      { path: '/events', element: <EventSearchPage /> },
      { path: '/events/:eventId', element: <EventDetailsPage /> },
      { path: '/events/:eventId/lifecycle', element: <EventLifecyclePage /> },
      { path: '/events/:eventId/report', element: <EventReportPage /> },
      { path: '/activities/:activityId/feedback', element: <GiveFeedbackPage /> },
      { path: '/activities/:activityId/attendance', element: <AttendancePage /> },
      { path: '/my-activities', element: <MyActivitiesPage /> },
      { path: '/notifications', element: <NotificationsPage /> },
      { path: '/signups/:signupId/qr', element: <QrPassPage /> },
    ],
  },
]
