import { Navigate, Route, Routes } from 'react-router-dom'
import Home from './Home'
import PosLogin from './pos/PosLogin'
import PosShell from './pos/PosShell'
import TerminalSetup from './pos/TerminalSetup'
import PortalLayout from './portal/PortalLayout'
import PortalLogin from './portal/PortalLogin'
import Customer360 from './portal/Customer360'
import Customers from './portal/Customers'
import Dashboard from './portal/Dashboard'
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
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="customers" element={<Customers />} />
        <Route path="customers/:id" element={<Customer360 />} />
        {/* Chức năng chưa làm: trang chờ */}
        {PORTAL_MENU.filter((m) => m.step !== 'B8').map((m) => (
          <Route key={m.key} path={m.key} element={<Placeholder title={m.label} step={m.step} />} />
        ))}
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
