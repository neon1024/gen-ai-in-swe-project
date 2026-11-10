import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('apiFetch', () => {
  it('US-22 resolves a 204 No Content response without parsing a body', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))
    await expect(apiFetch<void>('/api/lifecycle/events/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
