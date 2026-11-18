import { useState } from 'react'
import { field } from './helpers'

const STEP_MINUTES = 15

/** Times of day every 15 minutes, as "HH:mm". */
const TIMES = Array.from({ length: (24 * 60) / STEP_MINUTES }, (_, i) => {
  const minutes = i * STEP_MINUTES
  return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`
})

function split(value: string): [string, string] {
  return value ? [value.slice(0, 10), value.slice(11, 16)] : ['', '']
}

/**
 * A date picker plus a time list, so no browser makes people type the hour. The combined local
 * value ("YYYY-MM-DDTHH:mm", empty until both parts are chosen) is submitted under `name`.
 */
export function DateTimeField({
  label,
  name,
  value,
  defaultValue = '',
  defaultDate = '',
  required = false,
  onChange,
}: {
  label: string
  name: string
  value?: string
  defaultValue?: string
  defaultDate?: string
  required?: boolean
  onChange?: (value: string) => void
}) {
  const [initialDate, initialTime] = split(value ?? defaultValue)
  const [date, setDate] = useState(initialDate || defaultDate)
  const [time, setTime] = useState(initialTime)
  const [seenValue, setSeenValue] = useState(value)

  // A new value from the parent (for example a suggested end time) replaces both parts.
  if (value !== seenValue) {
    setSeenValue(value)
    if (value) {
      const [nextDate, nextTime] = split(value)
      setDate(nextDate)
      setTime(nextTime)
    }
  }

  function update(nextDate: string, nextTime: string) {
    setDate(nextDate)
    setTime(nextTime)
    onChange?.(nextDate && nextTime ? `${nextDate}T${nextTime}` : '')
  }

  const times = time && !TIMES.includes(time) ? [...TIMES, time].sort() : TIMES
  const combined = date && time ? `${date}T${time}` : ''

  return (
    <fieldset role="group" aria-label={label} className="min-w-0">
      <legend className="text-sm font-medium">{label}</legend>
      <div className="mt-1 flex gap-2">
        <input
          type="date"
          aria-label={`${label} date`}
          required={required}
          value={date}
          onChange={(e) => update(e.target.value, time)}
          className={`${field} mt-0 flex-1`}
        />
        <select
          aria-label={`${label} time`}
          required={required || Boolean(date)}
          value={time}
          onChange={(e) => update(date, e.target.value)}
          className={`${field} mt-0 w-28`}
        >
          <option value="">--:--</option>
          {times.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
      </div>
      <input type="hidden" name={name} value={combined} />
    </fieldset>
  )
}
