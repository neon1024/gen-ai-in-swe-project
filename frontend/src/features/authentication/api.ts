import {apiFetch} from '../event-lifecycle/api/client'

export type LoginResponse = {
  userId: string
  email: string
  username: string
  displayName: string
  status: string
}

export function register(values: {
  email: string
  password: string
  username: string
  displayName: string
}) {
  return apiFetch<void>('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(values),
  })
}

export async function verifyEmail(token: string): Promise<void> {
  await apiFetch<void>('/api/auth/verification/confirm', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ token }),
  })
}

export function resendVerificationEmail(email: string) {
  return apiFetch<void>('/api/auth/verification/resend', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  })
}

export function login(email: string, password: string) {
  return apiFetch<LoginResponse>('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
}

export function logout() {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' })
}
