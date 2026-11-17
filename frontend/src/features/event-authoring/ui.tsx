import type { AuthoringError } from './api'

export function ErrorBox({ error }: { error: AuthoringError | string | null }) {
  if (!error) return null
  const text = typeof error === 'string' ? error : error.message
  const violations = typeof error === 'string' ? [] : error.violations
  return (
    <div role="alert" className="mb-4 rounded bg-red-100 p-3 text-red-800">
      <p>{text}</p>
      {violations.length > 0 && (
        <ul className="list-disc pl-5">
          {violations.map((v, i) => <li key={i}>{v.message}</li>)}
        </ul>
      )}
    </div>
  )
}
