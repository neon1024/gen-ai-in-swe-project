import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { ApiError } from '../api/client'
import { checkInQr, getAttendance, markAttendance } from '../api/participant'
import type { AttendanceParticipant } from '../api/types'

export function AttendancePage() {
  const { activityId = '' } = useParams()
  const [people, setPeople] = useState<AttendanceParticipant[] | null>(null)
  const [query, setQuery] = useState('')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  useEffect(() => { getAttendance(activityId).then(setPeople).catch((e: unknown) => setError(e instanceof ApiError ? e.message : 'Attendance could not be loaded.')) }, [activityId])
  async function search() { try { setPeople(await getAttendance(activityId, query)); setError(null) } catch (e) { setError(e instanceof ApiError ? e.message : 'Search failed.') } }
  async function scan() { if (!code.trim()) return; try { const result = await checkInQr(activityId, code.trim()); setPeople((current) => current?.map((person) => person.signupId === result.signupId ? result : person) ?? [result]); setMessage(`${result.displayName} is marked present.`); setCode(''); setError(null) } catch (e) { setError(e instanceof ApiError ? e.message : 'This code could not be checked in.') } }
  async function toggle(person: AttendanceParticipant) { try { const result = await markAttendance(person.signupId, person.status !== 'ATTENDED'); setPeople((current) => current?.map((item) => item.signupId === result.signupId ? result : item) ?? null); setError(null) } catch (e) { setError(e instanceof ApiError ? e.message : 'Attendance could not be updated.') } }
  return <main className="page"><p className="eyebrow">Staff tools</p><h1>Check in participants</h1><p className="lede">Search a name or scan a personal pass. Corrections stay open for seven days after the activity.</p>{error && <p role="alert" className="notice error">{error}</p>}{message && <p className="notice success">{message}</p>}<section className="scan-panel"><label htmlFor="qr-code">QR code value</label><div className="inline-form"><input id="qr-code" value={code} onChange={(e) => setCode(e.target.value)} placeholder="Paste or scan code" /><button className="button primary" onClick={scan}>Check in</button></div></section><section className="attendance-section"><div className="inline-form"><label className="sr-only" htmlFor="participant-search">Search participants</label><input id="participant-search" value={query} onChange={(e) => setQuery(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && search()} placeholder="Search by name or username" /><button className="button secondary" onClick={search}>Search</button></div>{people === null && !error && <p className="loading">Loading participants…</p>}{people?.length === 0 && <p className="empty small">No confirmed participants match that search.</p>}<div className="participant-list">{people?.map((person) => <div className="participant" key={person.signupId}><div><strong>{person.displayName}</strong><span>@{person.username}</span></div><button className={person.status === 'ATTENDED' ? 'button secondary' : 'button primary'} onClick={() => toggle(person)}>{person.status === 'ATTENDED' ? 'Undo present' : 'Mark present'}</button></div>)}</div></section></main>
}
