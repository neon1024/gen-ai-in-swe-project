import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { ApiError } from '../api/client'
import { getPersonalQr } from '../api/participant'
import type { PersonalQrPass } from '../api/types'

export function QrPassPage() {
  const { signupId = '' } = useParams()
  const [pass, setPass] = useState<PersonalQrPass | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { getPersonalQr(signupId).then(setPass).catch((e: unknown) => setError(e instanceof ApiError ? e.message : 'Your pass could not be loaded.')) }, [signupId])
  return <main className="page pass-page"><p className="eyebrow">Your entry pass</p><h1>Show up ready.</h1>{error && <p role="alert" className="notice error">{error}</p>}{!pass && !error && <p className="loading">Preparing your pass…</p>}{pass && <article className="qr-card"><p className="status confirmed">Confirmed</p><h2>{pass.activityTitle}</h2><p>{new Date(pass.startsAt).toLocaleString(undefined, { dateStyle: 'full', timeStyle: 'short' })}</p>{pass.qrCodeType === 'image' ? <img className="qr-image" src={pass.qrCode} alt={`Personal QR code for ${pass.activityTitle}`} /> : <code className="qr-text">{pass.qrCode}</code>}<p className="muted">This pass belongs to you. Keep it private and show it at the door.</p></article>}</main>
}
