import { apiFetch, request } from './client'
import type { EventReport } from './types'

export function getReport(eventId: string): Promise<EventReport> {
  return apiFetch<EventReport>(`/api/reports/events/${eventId}`)
}

/** Fetches the CSV with the shared auth header and saves it through a temporary download link. */
export async function downloadReportCsv(eventId: string): Promise<void> {
  const response = await request(`/api/reports/events/${eventId}/csv`)
  const url = URL.createObjectURL(await response.blob())
  try {
    const link = document.createElement('a')
    link.href = url
    link.download = `event-${eventId}-report.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
  } finally {
    URL.revokeObjectURL(url)
  }
}
