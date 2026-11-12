import { Navigate, Route, Routes } from 'react-router-dom'
import Home from './Home'
import PosLogin from './pos/PosLogin'
import PosShell from './pos/PosShell'
import TerminalSetup from './pos/TerminalSetup'
import PortalLayout from './portal/PortalLayout'
import PortalLogin from './portal/PortalLogin'
import Placeholder from './portal/Placeholder'
import { PORTAL_MENU } from './portal/menu'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />

      {/* POS – màn hình thu ngân */}
      <Route path="/pos/setup" element={<TerminalSetup />} />
      <Route path="/pos/login" element={<PosLogin />} />
      <Route path="/pos" element={<PosShell />} />

      {/* Web Portal */}
      <Route path="/portal/login" element={<PortalLogin />} />
      <Route path="/portal" element={<PortalLayout />}>
        {PORTAL_MENU.map((m) => (
          <Route key={m.key} path={m.key} element={<Placeholder title={m.label} step={m.step} />} />
        ))}
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
