import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { ApiError } from '../../event-lifecycle/api/client'
import { login, register } from '../api'
import { Button } from '../../../components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card'
import { Input } from '../../../components/ui/input'
import { Label } from '../../../components/ui/label'
import { useAuth } from '../context'

export default function AuthPage() {
  const isRegister = useLocation().pathname === '/signup'
  const navigate = useNavigate()
  const { isAuthenticated, setUser } = useAuth()
  const [values, setValues] = useState({ email: '', password: '', username: '', displayName: '' })
  const [loading, setLoading] = useState(false)
  const [confirmPassword, setConfirmPassword] = useState('')

  const passwordTooShort =
    isRegister && values.password.length > 0 && values.password.length < 8

  const passwordsDoNotMatch =
    isRegister &&
    confirmPassword.length > 0 &&
    values.password !== confirmPassword

  const signupInvalid =
    isRegister &&
    (
      values.password.length < 8 ||
      confirmPassword.length === 0 ||
      values.password !== confirmPassword
    )

  useEffect(() => {
    if (isAuthenticated) navigate('/events', { replace: true })
  }, [isAuthenticated, navigate])

  if (isAuthenticated) return null

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    if (isRegister && signupInvalid) {
      return
    }

    setLoading(true)
    try {
      if (isRegister) {
        await register(values)
        toast.success('Account created. You can now sign in.')
        navigate('/check-email', {
          state: { email: values.email }
        })
      } else {
        const session = await login(values.email, values.password)
        setUser(session)
        toast.success(`Welcome, ${session.displayName}`)
        navigate('/')
      }
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : 'Unable to complete the request')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <CardHeader><CardTitle>{isRegister ? 'Create your campus account' : 'Welcome back'}</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="form-stack">
            {isRegister && (
              <>
                <Label htmlFor="displayName">Display name</Label>
                <Input id="displayName" required value={values.displayName} onChange={(e) => setValues({ ...values, displayName: e.target.value })} />
                <Label htmlFor="username">Username</Label>
                <Input id="username" required value={values.username} onChange={(e) => setValues({ ...values, username: e.target.value })} />
              </>
            )}
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" required value={values.email} onChange={(e) => setValues({ ...values, email: e.target.value })} />
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              required
              minLength={8}
              value={values.password}
              aria-invalid={passwordTooShort}
              aria-describedby={
                passwordsDoNotMatch || passwordTooShort
                  ? 'password-validation'
                  : undefined
              }
              onChange={(e) => setValues({ ...values, password: e.target.value })}
            />
            {isRegister && (
              <>
                <Label htmlFor="confirmPassword">Confirm password</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  required
                  minLength={8}
                  value={confirmPassword}
                  aria-invalid={passwordsDoNotMatch}
                  aria-describedby={
                    passwordsDoNotMatch || passwordTooShort
                      ? 'password-validation'
                      : undefined
                  }
                  onChange={(event) => setConfirmPassword(event.target.value)}
                />
              </>
            )}
            {passwordTooShort ? (
              <p id="password-validation" role="alert" className="field-error">
                Password must contain at least 8 characters.
              </p>
            ) : passwordsDoNotMatch ? (
              <p id="password-validation" role="alert" className="field-error">
                Passwords do not match.
              </p>
            ) : null}
            <Button type="submit" disabled={loading || (isRegister && signupInvalid)}>
              {loading ? 'Working...' : isRegister ? 'Create account' : 'Sign in'}
            </Button>
          </form>
          <p className="auth-switch">
            {isRegister ? 'Already have an account?' : 'New to UBB Events?'}{' '}
            <Link to={isRegister ? '/login' : '/signup'}>{isRegister ? 'Sign in' : 'Create one'}</Link>
          </p>
        </CardContent>
      </Card>
    </main>
  )
}