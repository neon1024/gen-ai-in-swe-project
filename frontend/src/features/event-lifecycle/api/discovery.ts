import { apiFetch } from './client'
import type { EventCategory, EventSearchResult } from './types'

export interface SearchParams {
  q?: string
  from?: string
  to?: string
  organizationId?: string
  category?: EventCategory
  page?: number
}

export function searchEvents(params: SearchParams): Promise<EventSearchResult> {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      query.set(key, String(value))
    }
  }
  const suffix = query.size > 0 ? `?${query}` : ''
  return apiFetch<EventSearchResult>(`/api/discovery/events${suffix}`)
}
