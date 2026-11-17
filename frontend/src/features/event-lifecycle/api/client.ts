const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

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

/** Sends the request with the shared headers and turns a non-2xx answer into an {@link ApiError}. */
export async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  // Provisional: the authentication share replaces the dev user header.
  const devUserId = localStorage.getItem('devUserId')
  if (devUserId) {
    headers.set('X-User-Id', devUserId)
  }
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      credentials: 'include',
      headers,
    })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'The backend is not reachable. Start Spring Boot on http://localhost:8080 and try again.')
  }
  if (response.status === 401) {
    window.dispatchEvent(new Event('auth:unauthorized'))
  }
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
  return response
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await request(path, init)
  const body = await response.text()
  if (!body) {
    return undefined as T
  }
  return JSON.parse(body) as T
}
