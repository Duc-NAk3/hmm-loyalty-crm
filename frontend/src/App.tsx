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
import Migration from './portal/Migration'
import Products from './portal/Products'
import SmsInbox from './portal/SmsInbox'
import Tiers from './portal/Tiers'
import Users from './portal/Users'

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
        <Route path="products" element={<Products />} />
        <Route path="tiers" element={<Tiers />} />
        <Route path="migration" element={<Migration />} />
        <Route path="users" element={<Users />} />
        <Route path="sms" element={<SmsInbox />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
