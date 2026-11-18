# Frontend

The client is a Vite + React + TypeScript single-page application. The Vite
development server proxies `/api` requests to `http://localhost:8080`.
Set `VITE_API_BASE_URL` when the API is hosted on another origin; leave it
unset for the local Vite proxy.

## Run locally

```bash
npm install
npm run dev
```

Open `http://localhost:5173`. Start the Spring Boot backend separately when
using API-backed views. Participant and attendance views require the contracts
implemented in `src/features/event-lifecycle/api/participant.ts`.

## Checks

```bash
npm run build
npm run lint
npm run test
```

Authentication is supplied by the backend session cookie. API clients echo the
`XSRF-TOKEN` cookie as the `X-XSRF-TOKEN` header on every request.
