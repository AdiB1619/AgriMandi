import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../features/auth/AuthContext';

/** Where a freshly authenticated user of each role belongs. */
export function landingPathFor(role: string | undefined | null): string {
  switch (role) {
    case 'ADMIN':
      return '/admin/dashboard';
    case 'FARMER':
      return '/farmer-portal';
    case 'BUYER':
      return '/buyer-portal';
    default:
      return '/dashboard';
  }
}

/**
 * UX guard for role-restricted screens.
 *
 * This is convenience, not security: it stops a user from landing on a screen built for a
 * different role and shows them a plain explanation instead of a broken page. The Spring Boot
 * API independently rejects unauthorized requests, so bypassing this component grants nothing.
 */
export default function RoleRoute({
  allow,
  children,
}: {
  allow: string[];
  children: ReactNode;
}) {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="p-8 text-body-md text-on-surface-variant">Checking your access…</div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (!allow.includes(user.role)) {
    return <AccessDenied role={user.role} allowed={allow} />;
  }

  return <>{children}</>;
}

function AccessDenied({ role, allowed }: { role: string; allowed: string[] }) {
  const { user } = useAuth();
  const home = landingPathFor(user?.role);

  return (
    <div className="bg-surface-container-lowest rounded-xl p-8 flex flex-col items-center justify-center text-center gap-3">
      <span className="material-symbols-outlined text-error" style={{ fontSize: '2.5rem' }}>
        lock
      </span>
      <p className="text-headline-sm font-semibold text-on-surface">Access denied</p>
      <p className="text-body-sm text-on-surface-variant max-w-md">
        This screen is available to {allowed.join(' and ')} accounts. You are signed in as{' '}
        <span className="font-semibold">{role}</span>, so it is not shown to you.
      </p>
      <a
        href={home}
        className="mt-2 inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-primary text-on-primary text-label-lg font-semibold hover:bg-primary-container transition-colors"
      >
        <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
          home
        </span>
        Go to my home screen
      </a>
    </div>
  );
}

/** Sends a signed-in user to the landing page for their own role. */
export function RoleLanding() {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return <div className="p-8 text-body-md text-on-surface-variant">Checking your access…</div>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <Navigate to={landingPathFor(user.role)} replace />;
}
