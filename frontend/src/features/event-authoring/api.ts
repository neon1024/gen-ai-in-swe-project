export type Category = 'Workshop' | 'Conference' | 'Training' | 'Social' | 'Competition' | 'Other'
export const CATEGORIES: Category[] = ['Workshop', 'Conference', 'Training', 'Social', 'Competition', 'Other']

export interface AgendaItem { id: string; time: string; title: string; speaker: string | null }
export interface Activity {
  id: string; title: string; description: string; start: string; end: string
  location: string | null; onlineLink: string | null; capacity: number
  registrationOpen: string | null; registrationClose: string | null
  visibility: string; cancelled: boolean; agenda: AgendaItem[]
}
export interface AuthoringEvent {
  id: string; organizationRef: string; title: string; description: string; category: Category
  state: 'DRAFT' | 'PUBLISHED' | 'COMPLETED' | 'CANCELLED'; version: number
  activities: Activity[]; capabilities: { canEdit: boolean; canPublish: boolean }
}
export interface Violation { path: string; code: string; message: string }

export class AuthoringError extends Error {
  readonly status: number
  readonly code: string
  readonly violations: Violation[]

  constructor(status: number, code: string, message: string, violations: Violation[]) {
    super(message)
    this.name = 'AuthoringError'
    this.status = status
    this.code = code
    this.violations = violations
  }
}

export type ActivityInput = Omit<Activity, 'id' | 'cancelled' | 'agenda'>
export type AgendaInput = Omit<AgendaItem, 'id'>

const BASE = '/api/event-authoring/events'

// The host session owns identity; Spring's cookie CSRF token is echoed back as a header.
function csrfHeaders(): Record<string, string> {
  const token = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='))?.slice(11)
  return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {}
}

async function call(path: string, method = 'GET', body?: unknown): Promise<AuthoringEvent> {
  const response = await fetch(`${BASE}${path}`, {
    method,
    credentials: 'same-origin',
    headers: { ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...csrfHeaders() },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    let p: { code?: string; message?: string; violations?: Violation[] } = {}
    try { p = await response.json() } catch { /* non-JSON error body */ }
    throw new AuthoringError(response.status, p.code ?? 'UNKNOWN', p.message ?? response.statusText, p.violations ?? [])
  }
  return (await response.json()) as AuthoringEvent
}

export const createDraft = (b: { title: string; description: string; category: Category; organizationRef: string }) =>
  call('', 'POST', b)
export const getEvent = (id: string) => call(`/${id}`)
export const editDetails = (id: string, v: number, b: { title: string; description: string; category: Category }) =>
  call(`/${id}`, 'PUT', { expectedVersion: v, ...b })
export const publish = (id: string, v: number) => call(`/${id}/publish`, 'POST', { expectedVersion: v })
export const saveActivity = (id: string, v: number, b: ActivityInput, activityId?: string) =>
  activityId
    ? call(`/${id}/activities/${activityId}`, 'PUT', { ...b, expectedVersion: v })
    : call(`/${id}/activities`, 'POST', { ...b, expectedVersion: v })
export const removeActivity = (id: string, a: string, v: number) => call(`/${id}/activities/${a}?expectedVersion=${v}`, 'DELETE')
export const saveAgenda = (id: string, a: string, v: number, b: AgendaInput, itemId?: string) =>
  itemId
    ? call(`/${id}/activities/${a}/agenda/${itemId}`, 'PUT', { ...b, expectedVersion: v })
    : call(`/${id}/activities/${a}/agenda`, 'POST', { ...b, expectedVersion: v })
export const removeAgenda = (id: string, a: string, item: string, v: number) =>
  call(`/${id}/activities/${a}/agenda/${item}?expectedVersion=${v}`, 'DELETE')
