import { render, screen } from '@testing-library/react'
import { test, expect } from 'vitest'
import App from './App'

test('provides an accessible campus navigation shell', () => {
  localStorage.setItem('ubb.events.session', JSON.stringify({
    userId: 'test-user',
    email: 'test@example.com',
    username: 'test-user',
    displayName: 'Test User',
    status: 'ACTIVE',
  }))
  render(<App />)
  expect(screen.getByRole('link', { name: 'UBB / together' })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Discover' })).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: 'Events', level: 1 })).toBeInTheDocument()
})
