import { NavLink, Outlet } from 'react-router'
import { CalendarDays, Compass, Inbox, Sparkles } from 'lucide-react'
import { Separator } from './ui/separator'

const links = [
  { to: '/events', label: 'Discover', icon: Compass },
  { to: '/my-activities', label: 'My activities', icon: CalendarDays },
  { to: '/notifications', label: 'Notifications', icon: Inbox },
]

export function AppShell() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/events" className="brand">
          <span className="brand-mark" aria-hidden="true"><Sparkles size={16} /></span>
          <span>UBB / together</span>
        </NavLink>
        <span className="topbar-note">A better way to show up</span>
      </header>
      <div className="shell-body">
        <aside className="sidebar" aria-label="Main navigation">
          <p className="sidebar-caption">Your campus</p>
          <nav className="nav-list">
            {links.map((link) => (
              <NavLink key={link.to} to={link.to} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
                <link.icon size={17} aria-hidden="true" />
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="sidebar-footer">
            <Separator />
            <p>Find your next room full of people.</p>
            <NavLink to="/events">Browse events</NavLink>
          </div>
        </aside>
        <div className="content"><Outlet /></div>
      </div>
    </div>
  )
}
