import { apiFetch } from './client'
import type {
  AttendanceParticipant,
  EventDetail,
  NotificationItem,
  PersonalQrPass,
  SignupSummary,
} from './types'

export function getEventDetails(eventId: string): Promise<EventDetail> {
  return apiFetch<EventDetail>(`/api/events/${eventId}`)
}

export function getMySignups(): Promise<SignupSummary[]> {
  return apiFetch<SignupSummary[]>('/api/registrations/me')
}

export function signup(activityId: string): Promise<SignupSummary> {
  return apiFetch<SignupSummary>(`/api/activities/${activityId}/signups`, { method: 'POST' })
}

export function cancelSignup(signupId: string): Promise<void> {
  return apiFetch<void>(`/api/signups/${signupId}`, { method: 'DELETE' })
}

export function getNotifications(): Promise<NotificationItem[]> {
  return apiFetch<NotificationItem[]>('/api/notifications')
}

export function getPersonalQr(signupId: string): Promise<PersonalQrPass> {
  return apiFetch<PersonalQrPass>(`/api/signups/${signupId}/qr`)
}

export function getAttendance(activityId: string, search?: string): Promise<AttendanceParticipant[]> {
  const query = search ? `?q=${encodeURIComponent(search)}` : ''
  return apiFetch<AttendanceParticipant[]>(`/api/attendance/activities/${activityId}/participants${query}`)
}

export function markAttendance(signupId: string, attended: boolean): Promise<AttendanceParticipant> {
  return apiFetch<AttendanceParticipant>(`/api/attendance/signups/${signupId}`, {
    method: attended ? 'POST' : 'DELETE',
  })
}

export function checkInQr(activityId: string, code: string): Promise<AttendanceParticipant> {
  return apiFetch<AttendanceParticipant>(`/api/attendance/activities/${activityId}/check-in`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code }),
  })
}
