import { useState } from 'react'
import { AuthoringError } from './api'

export const field = 'mt-1 block w-full rounded border border-gray-300 p-2'
export const btn = 'rounded border border-gray-400 px-3 py-1'

export function messageOf(e: unknown): AuthoringError | string {
  return e instanceof AuthoringError ? e : 'Something went wrong. Please try again.'
}

export function useRun() {
  const [error, setError] = useState<AuthoringError | string | null>(null)
  async function run(action: () => Promise<void>) {
    try {
      await action()
      setError(null)
    } catch (e) {
      setError(messageOf(e))
    }
  }
  return { error, run }
}

/** ISO instant -> value for <input type="datetime-local"> in the browser's zone. */
export function toLocalInput(iso: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

export function fromLocalInput(v: string): string | null {
  return v ? new Date(v).toISOString() : null
}
