
import { useLocation, useNavigate } from 'react-router'
import { Button } from '../../../components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card'

export default function CheckEmailPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const email = location.state?.email

    return (
        <main className="auth-page">
            <Card className="auth-card">
                <CardHeader>
                    <CardTitle>Check your email</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="form-stack">
                        <p>
                            We've sent a verification link to{' '}
                            <strong>{email || 'your email address'}</strong>.
                        </p>
                        <p>
                            Please check your inbox and follow the link
                            to verify your account.
                        </p>
                        <Button onClick={() => navigate('/login', { replace: true })}>
                            Back to login
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </main>
    )
}
