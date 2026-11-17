import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { ApiError } from '../../event-lifecycle/api/client'
import { verifyEmail, resendVerificationEmail } from '../api'
import { Button } from '../../../components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card'

export default function VerifyAccountPage() {
    const location = useLocation()
    const navigate = useNavigate()

    const token = new URLSearchParams(location.search).get('token') ?? ''

    const [loading, setLoading] = useState(true)
    const [verified, setVerified] = useState(false)
    const [email, setEmail] = useState('')
    const [resending, setResending] = useState(false)

    useEffect(() => {
        if (!token) {
            setLoading(false)
            return
        }

        async function handleVerification() {
            try {
                await verifyEmail(token)
                setVerified(true)
                toast.success('Your email has been verified successfully.')
            } catch (error) {
                toast.error(
                    error instanceof ApiError
                        ? error.message
                        : 'Unable to verify your email address'
                )
            } finally {
                setLoading(false)
            }
        }

        handleVerification()
    }, [token])

    async function handleResend() {
        setResending(true)

        try {
            await resendVerificationEmail(email)
            toast.success('Verification email sent. Please check your inbox.')
        } catch (error) {
            toast.error(
                error instanceof ApiError
                    ? error.message
                    : 'Unable to resend verification email'
            )
        } finally {
            setResending(false)
        }
    }

    return (
        <main className="auth-page">
            <Card className="auth-card">
                <CardHeader>
                    <CardTitle>
                        {loading
                            ? 'Verifying your email'
                            : verified
                                ? 'Email verified'
                                : 'Email verification'}
                    </CardTitle>
                </CardHeader>

                <CardContent>
                    <div className="form-stack">
                        {loading ? (
                            <p>Please wait while we verify your email address...</p>
                        ) : verified ? (
                            <>
                                <p>
                                    Your email address has been successfully verified.
                                    Your account is now active.
                                </p>

                                <Button
                                    type="button"
                                    onClick={() => navigate('/login', { replace: true })}
                                >
                                    Continue
                                </Button>
                            </>
                        ) : (
                            <>
                                <p>
                                    {!token
                                        ? 'The verification link is missing or invalid.'
                                        : 'Your verification link is invalid or has expired. Please request a new verification email.'}
                                </p>

                                <input
                                    type="email"
                                    placeholder="Enter your email address"
                                    value={email}
                                    onChange={(event) => setEmail(event.target.value)}
                                    required
                                />

                                <Button
                                    type="button"
                                    onClick={handleResend}
                                    disabled={resending || !email.trim()}
                                >
                                    {resending ? 'Sending...' : 'Resend verification email'}
                                </Button>
                            </>
                        )}
                    </div>

                    {!loading && (
                        <p className="auth-switch">
                            <Link to="/login">Back to sign in</Link>
                        </p>
                    )}
                </CardContent>
            </Card>
        </main>
    )
}
