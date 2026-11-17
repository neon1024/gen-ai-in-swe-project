import type { RouteObject } from 'react-router'
import { OrganizationMembershipPage } from './OrganizationMembershipPage'

export const organizationMembershipRoutes: RouteObject[] = [
  { path: '/organizations', element: <OrganizationMembershipPage /> },
]
