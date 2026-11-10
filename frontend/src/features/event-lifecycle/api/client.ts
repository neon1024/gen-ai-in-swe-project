export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

interface ProblemDetail {
  title?: string
  detail?: string
  code?: string
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  // Provisional: the authentication share replaces the dev user header.
  const devUserId = localStorage.getItem('devUserId')
  if (devUserId) {
    headers.set('X-User-Id', devUserId)
  }
  const response = await fetch(path, { ...init, headers })
  if (!response.ok) {
    let problem: ProblemDetail = {}
    try {
      problem = await response.json()
    } catch {
      // Body is not JSON: fall back to the status text.
    }
    throw new ApiError(
      response.status,
      problem.code ?? 'UNKNOWN',
      problem.detail ?? problem.title ?? response.statusText,
    )
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
