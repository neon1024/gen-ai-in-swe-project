import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './client'
import { downloadReportCsv } from './reports'

const EVENT_ID = '0d5c4a52-6a0e-4a38-9f3e-2f0f1c8a7b11'

beforeEach(() => {
  localStorage.clear()
  URL.createObjectURL = vi.fn(() => 'blob:report')
  URL.revokeObjectURL = vi.fn()
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  localStorage.clear()
})

describe('downloadReportCsv', () => {
  it('US-50 fetches the CSV with the user header and saves it through an object URL', async () => {
    localStorage.setItem('devUserId', 'user-7')
    const fetchMock = vi.fn().mockResolvedValue(new Response('a,b\n1,2\n', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      expect(this.download).toBe(`event-${EVENT_ID}-report.csv`)
      expect(this.href).toBe('blob:report')
    })

    await downloadReportCsv(EVENT_ID)

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe(`/api/reports/events/${EVENT_ID}/csv`)
    expect(new Headers(init.headers).get('X-User-Id')).toBe('user-7')
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1)
    expect(click).toHaveBeenCalledTimes(1)
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:report')
  })

  it('US-50 raises an ApiError when the server refuses the download', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ code: 'UNAUTHENTICATED', detail: 'Sign in first' }), { status: 401 }),
      ),
    )

    await expect(downloadReportCsv(EVENT_ID)).rejects.toMatchObject({
      status: 401,
      code: 'UNAUTHENTICATED',
    })
    await expect(downloadReportCsv(EVENT_ID)).rejects.toBeInstanceOf(ApiError)
    expect(URL.createObjectURL).not.toHaveBeenCalled()
  })
})
