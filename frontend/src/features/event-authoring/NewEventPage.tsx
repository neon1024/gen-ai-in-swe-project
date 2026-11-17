import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { CATEGORIES, createDraft, type Category } from './api'
import { field, useRun } from './helpers'
import { ErrorBox } from './ui'

export function NewEventPage() {
  const navigate = useNavigate()
  const { error, run } = useRun()
  const [category, setCategory] = useState<Category>('Workshop')

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    void run(async () => {
      const created = await createDraft({
        title: String(f.get('title')), description: String(f.get('description')),
        category, organizationRef: String(f.get('organizationRef')),
      })
      navigate(`/authoring/events/${created.id}`)
    })
  }

  return (
    <main className="mx-auto max-w-2xl p-4">
      <h1 className="mb-4 text-2xl font-semibold">Create a draft event</h1>
      <ErrorBox error={error} />
      <form onSubmit={submit} className="space-y-3">
        <label className="block">Title<input name="title" required className={field} /></label>
        <label className="block">Description<textarea name="description" required className={field} /></label>
        <label className="block">Category
          <select value={category} onChange={(e) => setCategory(e.target.value as Category)} className={field}>
            {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
          </select>
        </label>
        <label className="block">Organization<input name="organizationRef" required className={field} /></label>
        <button type="submit" className="rounded bg-blue-700 px-4 py-2 text-white">Create draft</button>
      </form>
    </main>
  )
}
