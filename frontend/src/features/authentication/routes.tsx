import type {RouteObject} from "react-router";
import AuthPage from "./pages/AuthPage.tsx";

export const authenticationRoutes: RouteObject[] = [
    { path: '/auth/register', element: <AuthPage /> },
    { path: '/auth/login', element: <AuthPage /> },
]
