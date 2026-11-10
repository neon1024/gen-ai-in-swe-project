import { apiFetch } from './client'
import type { LifecycleEventView } from './types'

export function getLifecycleEvent(id: string): Promise<LifecycleEventView> {
  return apiFetch<LifecycleEventView>(`/api/lifecycle/events/${id}`)
}

export function cancelEvent(id: string): Promise<LifecycleEventView> {
  return apiFetch<LifecycleEventView>(`/api/lifecycle/events/${id}/cancel`, { method: 'POST' })
}

export function cancelActivity(activityId: string): Promise<LifecycleEventView> {
  return apiFetch<LifecycleEventView>(`/api/lifecycle/activities/${activityId}/cancel`, { method: 'POST' })
}

export function deleteDraft(id: string): Promise<void> {
  return apiFetch<void>(`/api/lifecycle/events/${id}`, { method: 'DELETE' })
}
