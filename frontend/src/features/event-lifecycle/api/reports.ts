import { apiFetch } from './client'
import type { EventReport } from './types'

export function getReport(eventId: string): Promise<EventReport> {
  return apiFetch<EventReport>(`/api/reports/events/${eventId}`)
}

export function reportCsvUrl(eventId: string): string {
  return `/api/reports/events/${eventId}/csv`
}
