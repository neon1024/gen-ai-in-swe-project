import { NavLink, Outlet } from 'react-router'

const links = [
  { to: '/events', label: 'Discover', icon: '◇' },
  { to: '/my-activities', label: 'My activities', icon: '◷' },
  { to: '/notifications', label: 'Notifications', icon: '○' },
]

export function AppShell() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/events" className="brand">
          <span className="brand-mark" aria-hidden="true">U</span>
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
                <span aria-hidden="true">{link.icon}</span>
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="sidebar-footer">
            <p>Find your next room full of people.</p>
            <NavLink to="/events">Browse events</NavLink>
          </div>
        </aside>
        <div className="content"><Outlet /></div>
      </div>
    </div>
  )
}
