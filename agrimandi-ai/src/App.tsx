import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./features/auth/AuthContext";
import DashboardLayout from "./components/layout/DashboardLayout";
import Login from "./features/auth/Login";
import Dashboard from "./features/dashboard/Dashboard";
import AuctionDesk from "./features/auction/AuctionDesk";
import LotsManagement from "./features/lots/LotsManagement";

import TransactionsBilling from "./features/transactions/TransactionsBilling";
import { FarmersDirectory } from "./features/farmers/FarmersDirectory";
import { BuyersDirectory } from "./features/buyers/BuyersDirectory";
import { PaymentLedger } from "./features/payments/PaymentLedger";
import { AssetLedger } from "./features/assets/AssetLedger";
import { Reports } from "./features/reports/Reports";
import { AdminDashboard } from "./features/admin/AdminDashboard";
import { AuditTrail } from "./features/audit/AuditTrail";
import { FarmerPortal } from "./features/farmers/FarmerPortal";
import { BuyerPortal } from "./features/buyers/BuyerPortal";
import RoleRoute, { RoleLanding } from "./components/layout/RoleRoute";

/**
 * One authoritative implementation per screen.
 *
 * RoleRoute is a UX guard only: it keeps a user from wandering into a screen built for
 * another role and gives them a clear message. The backend remains the authority, and
 * every screen below reads its data from an authenticated API call.
 */
function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<DashboardLayout />}>
            <Route index element={<RoleLanding />} />

            {/* Operational screens — AGENT and ADMIN */}
            <Route
              path="dashboard"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <Dashboard />
                </RoleRoute>
              }
            />
            <Route
              path="auction-desk"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <AuctionDesk />
                </RoleRoute>
              }
            />
            <Route
              path="lots-management"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <LotsManagement />
                </RoleRoute>
              }
            />
            <Route
              path="transactions-billing"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <TransactionsBilling />
                </RoleRoute>
              }
            />
            <Route
              path="transactions-billing/:id"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <TransactionsBilling />
                </RoleRoute>
              }
            />
            <Route
              path="farmers"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <FarmersDirectory />
                </RoleRoute>
              }
            />
            <Route
              path="buyers"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <BuyersDirectory />
                </RoleRoute>
              }
            />

            {/* Settlement & inventory — both operational roles */}
            <Route
              path="payment-ledger"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <PaymentLedger />
                </RoleRoute>
              }
            />
            <Route
              path="returnable-asset-ledger"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <AssetLedger />
                </RoleRoute>
              }
            />
            <Route
              path="admin/reports"
              element={
                <RoleRoute allow={['AGENT', 'ADMIN']}>
                  <Reports />
                </RoleRoute>
              }
            />

            {/* APMC oversight — ADMIN only (the backend enforces this too) */}
            <Route
              path="admin/dashboard"
              element={
                <RoleRoute allow={['ADMIN']}>
                  <AdminDashboard />
                </RoleRoute>
              }
            />
            <Route
              path="admin/audit"
              element={
                <RoleRoute allow={['ADMIN']}>
                  <AuditTrail />
                </RoleRoute>
              }
            />

            {/* Participant portals — scoped to the signed-in principal server-side */}
            <Route
              path="farmer-portal"
              element={
                <RoleRoute allow={['FARMER']}>
                  <FarmerPortal />
                </RoleRoute>
              }
            />
            <Route
              path="buyer-portal"
              element={
                <RoleRoute allow={['BUYER']}>
                  <BuyerPortal />
                </RoleRoute>
              }
            />

            <Route path="*" element={<RoleLanding />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
