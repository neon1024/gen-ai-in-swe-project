import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { CATEGORIES, createDraft, type Category } from './api'
import { field, useRun } from './helpers'
import { ErrorBox } from './ui'
import { organizationMembershipApi, type OrganizationMembership } from '../organization-membership/organizationMembershipService'

export function NewEventPage() {
  const navigate = useNavigate()
  const { error, run } = useRun()
  const [category, setCategory] = useState<Category>('Workshop')
  const [organizations, setOrganizations] = useState<OrganizationMembership[] | null>(null)

  // Only organizers and admins may create drafts (BR-04), so offer just those organizations.
  useEffect(() => {
    void run(async () => {
      const all = await organizationMembershipApi.listOrganizations()
      setOrganizations(all.filter((o) => o.role === 'ORGANIZER' || o.role === 'ORG_ADMIN'))
    })
    // run is recreated each render; load once on mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

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
    <main className="mx-auto max-w-2xl space-y-4 p-4">
      <header>
        <p className="text-sm font-medium uppercase tracking-wide text-gray-600">New event</p>
        <h1 className="text-2xl font-semibold">Create a draft event</h1>
        <p className="mt-2 text-gray-700">
          Start with the basics. After creating the draft you add activities and publish it.
        </p>
      </header>
      <ErrorBox error={error} />
      <form onSubmit={submit} className="space-y-4 rounded-lg border border-gray-200 bg-white p-4">
        <label className="block">Organization
          <select name="organizationRef" required className={field}>
            {organizations?.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
          </select>
        </label>
        {organizations?.length === 0 && (
          <p role="status" className="text-sm text-gray-700">
            You need the Organizer or Org Admin role in an organization to create events.
          </p>
        )}
        <label className="block">Title<input name="title" required placeholder="For example: Intro to machine learning" className={field} /></label>
        <label className="block sm:w-64">Category
          <select value={category} onChange={(e) => setCategory(e.target.value as Category)} className={field}>
            {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
          </select>
        </label>
        <label className="block">Description<textarea name="description" required rows={4} placeholder="What will participants do or learn?" className={field} /></label>
        <div className="flex justify-end">
          <button type="submit" disabled={!organizations?.length} className="rounded bg-blue-700 px-4 py-2 text-white disabled:opacity-50">Create draft</button>
        </div>
      </form>
    </main>
  )
}
