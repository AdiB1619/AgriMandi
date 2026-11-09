import { useEffect, useState } from 'react';
import { Navigate, Outlet, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../features/auth/AuthContext';
import { api } from '../../services/api';

interface NavItem {
  to: string;
  label: string;
  icon: string;
}

const NAV_SECTIONS: Record<string, { heading: string; items: NavItem[] }[]> = {
  AGENT: [
    {
      heading: 'Trading Floor & Operations',
      items: [
        { to: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
        { to: '/auction-desk', label: 'Auction Desk', icon: 'gavel' },
        { to: '/lots-management', label: 'Lots Management', icon: 'inventory_2' },
        { to: '/transactions-billing', label: 'Transactions & Billing', icon: 'receipt_long' },
        { to: '/farmers', label: 'Farmers', icon: 'group' },
        { to: '/buyers', label: 'Buyers', icon: 'storefront' },
      ],
    },
    {
      heading: 'Settlement & Inventory',
      items: [
        { to: '/payment-ledger', label: 'Payment Ledger', icon: 'payments' },
        { to: '/returnable-asset-ledger', label: 'Returnable Asset Ledger', icon: 'local_shipping' },
        { to: '/admin/reports', label: 'Reports & Analytics', icon: 'analytics' },
      ],
    },
  ],
  ADMIN: [
    {
      heading: 'APMC Oversight',
      items: [
        { to: '/admin/dashboard', label: 'Oversight Dashboard', icon: 'monitoring' },
        { to: '/admin/reports', label: 'Reports & Analytics', icon: 'analytics' },
        { to: '/admin/audit', label: 'Audit Trail', icon: 'history' },
      ],
    },
    {
      heading: 'Settlement & Inventory',
      items: [
        { to: '/payment-ledger', label: 'Payment Ledger', icon: 'payments' },
        { to: '/returnable-asset-ledger', label: 'Returnable Asset Ledger', icon: 'local_shipping' },
      ],
    },
  ],
  FARMER: [
    {
      heading: 'Farmer Portal',
      items: [{ to: '/farmer-portal', label: 'Digital Passbook', icon: 'menu_book' }],
    },
  ],
  BUYER: [
    {
      heading: 'Buyer Portal',
      items: [{ to: '/buyer-portal', label: 'Purchase Ledger', icon: 'receipt_long' }],
    },
  ],
};

export default function DashboardLayout() {
  const { user, logout, isLoading } = useAuth();
  const location = useLocation();
  const [marketName, setMarketName] = useState<string | null>(null);

  const role = user?.role ?? '';
  const sections = NAV_SECTIONS[role] ?? [];

  // The market is a real attribute of the signed-in principal. If it cannot be read we say so
  // rather than inventing a yard name.
  useEffect(() => {
    if (!user?.marketId) {
      setMarketName(null);
      return;
    }
    let active = true;
    api
      .get<{ name?: string; code?: string }>('/api/markets/me')
      .then((market) => {
        if (active) setMarketName(market?.name ?? null);
      })
      .catch(() => {
        if (active) setMarketName(null);
      });
    return () => {
      active = false;
    };
  }, [user?.marketId]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center text-body-md text-on-surface-variant">
        Loading session…
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return (
    <div className="bg-background text-on-surface antialiased min-h-screen flex text-body-md font-sans">
      <aside className="fixed left-0 top-0 bottom-0 w-72 bg-surface-container-lowest flex flex-col z-50 shadow-[0_1px_8px_rgba(0,0,0,0.04)]">
        <div className="h-16 px-space-base flex items-center gap-space-sm bg-surface-container-low shrink-0">
          <div className="flex flex-col min-w-0">
            <div className="flex items-center gap-space-xs">
              <span className="text-headline-sm text-primary tracking-tight truncate">AgriMandi AI</span>
              <span className="px-space-xs py-space-xxs bg-primary-container text-on-primary text-label-sm rounded uppercase">
                APMC
              </span>
            </div>
            <span className="text-body-sm text-on-surface-variant truncate">Operational Suite v4.2</span>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto px-space-sm py-space-sm">
          {sections.map((section) => (
            <div key={section.heading}>
              <div className="px-space-xs py-space-xxs mb-space-xs text-label-sm text-on-surface-variant uppercase tracking-wider">
                {section.heading}
              </div>
              <nav className="flex flex-col gap-space-xxs mb-space-md">
                {section.items.map((item) => {
                  const active =
                    location.pathname === item.to ||
                    (item.to !== '/dashboard' && location.pathname.startsWith(`${item.to}/`));
                  return (
                    <Link
                      key={item.to}
                      to={item.to}
                      aria-current={active ? 'page' : undefined}
                      className={`flex items-center gap-space-sm px-space-sm py-space-xs rounded-lg text-label-lg transition-colors ${
                        active
                          ? 'bg-primary-container text-on-primary shadow-sm'
                          : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                      }`}
                    >
                      <span className="material-symbols-outlined text-space-lg shrink-0">{item.icon}</span>
                      <span className="truncate">{item.label}</span>
                    </Link>
                  );
                })}
              </nav>
            </div>
          ))}
        </div>

        <div className="p-space-sm bg-surface-container-low shrink-0">
          <div className="p-space-sm bg-surface-container-lowest rounded-xl flex flex-col gap-space-xs">
            <div className="flex items-start gap-space-sm">
              <div className="min-w-0 flex-1">
                <div className="text-label-lg text-on-surface truncate font-semibold">{user.username}</div>
                <div className="text-body-sm text-secondary truncate font-medium">Role: {user.role}</div>
              </div>
            </div>
            <div className="flex items-center justify-between pt-space-xs bg-surface-container-lowest">
              <span className="inline-flex items-center gap-space-xxs text-secondary text-label-sm">
                <span className="w-2 h-2 rounded-full bg-secondary" />
                Active Session
              </span>
              <button
                type="button"
                onClick={logout}
                className="inline-flex items-center gap-space-xxs text-label-sm text-error hover:bg-error-container hover:text-on-error-container px-space-xs py-space-xxs rounded transition-colors cursor-pointer border-none bg-transparent"
              >
                <span className="material-symbols-outlined text-space-base">logout</span>
                Exit
              </button>
            </div>
          </div>
        </div>
      </aside>

      <div className="pl-72 w-full">
        <header className="fixed top-0 left-72 right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-xl z-40 px-space-lg flex items-center justify-between gap-space-md shadow-[0_1px_8px_rgba(0,0,0,0.04)]">
          <div className="flex items-center gap-space-md min-w-0">
            <div className="flex items-center gap-space-xs bg-surface-container-low px-space-sm py-space-xs rounded-lg">
              <span className="material-symbols-outlined text-space-lg text-primary shrink-0">location_on</span>
              <div className="flex flex-col">
                <span className="text-label-sm text-on-surface-variant font-medium leading-none">
                  Market Location
                </span>
                <div className="flex items-center gap-space-xs mt-space-xxs">
                  <span className="text-label-lg text-on-surface font-semibold truncate">
                    {marketName ?? 'Not assigned'}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-space-sm shrink-0">
            {(role === 'AGENT' || role === 'ADMIN') && (
              <Link
                to="/lots-management?create=1"
                className="inline-flex items-center gap-space-xs px-space-sm py-space-xs bg-primary-container text-on-primary text-label-lg font-semibold rounded-lg hover:bg-primary transition-colors shadow-sm shrink-0"
              >
                <span className="material-symbols-outlined text-space-base">add_circle</span>
                <span>+ Create Lot</span>
              </Link>
            )}
            <div className="hidden md:flex items-center gap-space-xs bg-surface-container-low px-space-sm py-space-xs rounded-lg">
              <span className="material-symbols-outlined text-space-base text-on-surface-variant">badge</span>
              <span className="text-label-sm font-semibold text-on-surface truncate">{user.username}</span>
            </div>
          </div>
        </header>

        <main className="relative pt-16 w-full min-h-screen bg-background p-space-lg">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
