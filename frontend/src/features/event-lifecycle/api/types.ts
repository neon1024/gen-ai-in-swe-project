export type EventCategory = 'WORKSHOP' | 'CONFERENCE' | 'TRAINING' | 'SOCIAL' | 'COMPETITION' | 'OTHER'
export type EventState = 'DRAFT' | 'PUBLISHED' | 'COMPLETED' | 'CANCELLED'
export type ActivityState = 'SCHEDULED' | 'CANCELLED'
export type ActivityVisibility = 'PUBLIC' | 'PRIVATE'

export interface EventSummary {
  id: string
  title: string
  description: string
  category: EventCategory
  organizationId: string
  organizationName: string
  nextActivityStartsAt: string | null
}

export interface OrganizationOption {
  id: string
  name: string
}

export interface EventSearchResult {
  items: EventSummary[]
  page: number
  size: number
  totalItems: number
  organizations: OrganizationOption[]
}

export interface ActivityView {
  id: string
  title: string
  startsAt: string | null
  endsAt: string | null
  state: ActivityState
}

export interface Permissions {
  canCancel: boolean
  canDelete: boolean
}

export interface LifecycleEventView {
  id: string
  title: string
  state: EventState
  activities: ActivityView[]
  permissions: Permissions
}

export interface FeedbackComment {
  rating: number
  comment: string | null
}

export interface FeedbackSummary {
  activityId: string
  activityTitle: string
  count: number
  averageRating: number | null
  distribution: Record<number, number>
  comments: FeedbackComment[]
}

export interface FeedbackEligibilityView {
  eligible: boolean
  reason: string | null
}

export interface GiveFeedbackRequest {
  rating: number
  comment?: string
}

export interface GiveFeedbackResponse {
  id: string
}

export interface ActivityReportRow {
  activityId: string
  title: string
  startsAt: string | null
  state: ActivityState
  confirmedSignups: number
  waitlistSize: number
  cancellations: number
  attendanceRate: number | null
  averageRating: number | null
  ratingCount: number
}

export interface EventReport {
  eventId: string
  eventTitle: string
  eventState: EventState
  activities: ActivityReportRow[]
}
