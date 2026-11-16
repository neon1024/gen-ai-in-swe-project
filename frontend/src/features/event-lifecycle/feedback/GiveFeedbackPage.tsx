import { useEffect, useState, type FormEvent } from 'react'
import { useParams } from 'react-router'
import { ApiError } from '../api/client'
import { getEligibility, giveFeedback } from '../api/feedback'
import type { FeedbackEligibilityView } from '../api/types'

const REASON_MESSAGES: Record<string, string> = {
  NOT_ATTENDED: 'Only participants marked present can give feedback.',
  FEEDBACK_NOT_OPEN: 'Feedback opens when the activity ends.',
  FEEDBACK_WINDOW_CLOSED: 'Feedback closed 7 days after the activity ended.',
  FEEDBACK_ALREADY_GIVEN: 'You have already rated this activity.',
  ACTIVITY_CANCELLED: 'This activity was cancelled.',
}

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : 'Something went wrong. Please try again.'
}

export function GiveFeedbackPage() {
  const { activityId = '' } = useParams()
  const [eligibility, setEligibility] = useState<FeedbackEligibilityView | null>(null)
  const [rating, setRating] = useState<number | null>(null)
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let current = true
    getEligibility(activityId)
      .then((data) => {
        if (current) {
          setEligibility(data)
        }
      })
      .catch((e: unknown) => {
        if (current) {
          setError(messageOf(e))
        }
      })
    return () => {
      current = false
    }
  }, [activityId])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (rating === null) {
      setError('Choose a rating from 1 to 5.')
      return
    }
    setSubmitting(true)
    try {
      await giveFeedback(activityId, { rating, comment })
      setError(null)
      setDone(true)
    } catch (e) {
      setError(e instanceof ApiError && e.code in REASON_MESSAGES ? REASON_MESSAGES[e.code] : messageOf(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="page feedback-page">
      <p className="eyebrow">Help the next person choose well</p>
      <h1>Give feedback</h1>
      {error && (
        <p role="alert" className="notice error">
          {error}
        </p>
      )}
      {done && <p className="notice success">Thank you for your feedback.</p>}
      {!done && eligibility && !eligibility.eligible && (
        <p className="notice muted-notice">
          {REASON_MESSAGES[eligibility.reason ?? ''] ?? 'You cannot give feedback for this activity.'}
        </p>
      )}
      {!done && eligibility?.eligible && (
        <form onSubmit={submit} className="feedback-form">
          <fieldset>
            <legend className="mb-1 font-medium">Rating</legend>
            <div className="rating-options">
              {[1, 2, 3, 4, 5].map((value) => (
                <label key={value} className="rating-option">
                  <input
                    type="radio"
                    name="rating"
                    value={value}
                    checked={rating === value}
                    onChange={() => setRating(value)}
                  />
                  {value}
                </label>
              ))}
            </div>
          </fieldset>
          <div>
            <label htmlFor="feedback-comment" className="mb-1 block font-medium">
              Comment (optional)
            </label>
            <textarea
              id="feedback-comment"
              maxLength={2000}
              rows={5}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              className="feedback-comment"
            />
          </div>
          <button type="submit" disabled={submitting} className="button primary">
            Submit feedback
          </button>
        </form>
      )}
    </main>
  )
}
