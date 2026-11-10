import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getSummary } from '../api/feedback'
import type { FeedbackSummary } from '../api/types'

const STARS = [5, 4, 3, 2, 1]

export function FeedbackPanel({ activityId, title }: { activityId: string; title: string }) {
  const [summary, setSummary] = useState<FeedbackSummary | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let current = true
    getSummary(activityId)
      .then((data) => {
        if (current) {
          setSummary(data)
        }
      })
      .catch((e: unknown) => {
        if (current) {
          setError(e instanceof ApiError ? e.message : 'Something went wrong. Please try again.')
        }
      })
    return () => {
      current = false
    }
  }, [activityId])

  return (
    <section aria-label={`Feedback for ${title}`} className="p-3">
      {error && (
        <p role="alert" className="text-red-800">
          {error}
        </p>
      )}
      {summary && (
        <>
          <p className="mb-2">
            {summary.averageRating === null
              ? 'No ratings yet.'
              : `Average rating: ${Number(summary.averageRating.toFixed(2))} (${summary.count} ${summary.count === 1 ? 'rating' : 'ratings'})`}
          </p>
          <ul className="mb-3 space-y-1">
            {STARS.map((star) => {
              const count = summary.distribution[star] ?? 0
              const share = summary.count > 0 ? (count / summary.count) * 100 : 0
              return (
                <li key={star} aria-label={`${star} ${star === 1 ? 'star' : 'stars'}`} className="flex items-center gap-2">
                  <span className="w-16">{star} {star === 1 ? 'star' : 'stars'}</span>
                  <span className="h-3 w-40 rounded bg-gray-200" aria-hidden="true">
                    <span className="block h-3 rounded bg-blue-600" style={{ width: `${share}%` }} />
                  </span>
                  <span>{count}</span>
                </li>
              )
            })}
          </ul>
          <ul className="space-y-1">
            {summary.comments
              .filter((c) => c.comment)
              .map((c, index) => (
                <li key={index}>
                  <span className="mr-2 font-medium">{c.rating}/5</span>
                  {c.comment}
                </li>
              ))}
          </ul>
        </>
      )}
    </section>
  )
}
