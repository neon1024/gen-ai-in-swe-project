import { apiFetch } from './client'
import type { FeedbackEligibilityView, FeedbackSummary, GiveFeedbackRequest, GiveFeedbackResponse } from './types'

export function getEligibility(activityId: string): Promise<FeedbackEligibilityView> {
  return apiFetch<FeedbackEligibilityView>(`/api/feedback/activities/${activityId}/eligibility`)
}

export function giveFeedback(activityId: string, request: GiveFeedbackRequest): Promise<GiveFeedbackResponse> {
  return apiFetch<GiveFeedbackResponse>(`/api/feedback/activities/${activityId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
}

export function getSummary(activityId: string): Promise<FeedbackSummary> {
  return apiFetch<FeedbackSummary>(`/api/feedback/activities/${activityId}/summary`)
}
