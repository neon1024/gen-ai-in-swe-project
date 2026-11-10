import { render, screen } from '@testing-library/react'
import { test, expect } from 'vitest'
import App from './App'

test('provides an accessible event authoring mount and honest unavailable state', () => {
  render(<App />)
  expect(screen.getByRole('main', { name: 'Event authoring' })).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: 'Event authoring', level: 1 })).toBeInTheDocument()
  expect(screen.getByRole('status')).toHaveTextContent('Authoring is unavailable until the required account and organization integrations are connected.')
})
