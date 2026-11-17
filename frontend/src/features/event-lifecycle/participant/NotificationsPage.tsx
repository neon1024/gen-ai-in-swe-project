import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getNotifications } from '../api/participant'
import type { NotificationItem } from '../api/types'

export function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { getNotifications().then(setItems).catch((e: unknown) => setError(e instanceof ApiError ? e.message : 'Notifications could not be loaded.')) }, [])
  return <main className="page"><p className="eyebrow">Stay in the loop</p><h1>Notifications</h1><p className="lede">Waitlist moves and activity reminders land here.</p>{error && <p role="alert" className="notice error">{error}</p>}{items === null && !error && <p className="loading">Loading notifications…</p>}{items?.length === 0 && <div className="empty"><strong>Nothing new.</strong><p>We’ll let you know when something changes.</p></div>}<div className="notification-list">{items?.map((item) => <article className={`notification ${item.read ? '' : 'unread'}`} key={item.id}><span className="notification-dot" aria-hidden="true" /><div><h2>{item.title}</h2><p>{item.message}</p><time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</time></div></article>)}</div></main>
}
