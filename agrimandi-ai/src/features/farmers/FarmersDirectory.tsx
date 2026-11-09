import { useState } from 'react';
import { api } from '../../services/api';
import { useAsyncData } from '../../hooks/useAsyncData';
import { PageHeader } from '../../components/ui/PageHeader';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { EmptyState } from '../../components/ui/EmptyState';

interface Farmer {
  id: string;
  farmerCode: string | null;
  name: string;
  village: string | null;
  phone: string | null;
  address: string | null;
  status: string | null;
}

/**
 * Read-only directory of the farmers registered in this market, from `GET /api/farmers`.
 *
 * <p>There is no backend endpoint for creating or editing a farmer, so this screen deliberately
 * offers no register or edit control. Search filters the records the server already returned;
 * it is not a second source of truth.
 */
export function FarmersDirectory() {
  const farmers = useAsyncData<Farmer[]>(() => api.get<Farmer[]>('/api/farmers'), []);
  const [search, setSearch] = useState('');

  const all = farmers.data ?? [];
  const term = search.trim().toLowerCase();
  const visible = term
    ? all.filter((farmer) =>
        [farmer.name, farmer.farmerCode, farmer.village, farmer.phone]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(term)),
      )
    : all;

  return (
    <div className="flex flex-col w-full pb-space-2xl gap-space-lg">
      <PageHeader
        title="Farmers Directory"
        subtitle="Kisan accounts registered with this market"
        icon="groups"
        badge={
          <span className="px-2 py-0.5 bg-primary text-on-primary text-label-sm rounded uppercase">
            {all.length} registered
          </span>
        }
        actions={
          <div className="flex items-center gap-1 bg-surface-container-low px-2 py-1.5 rounded-lg">
            <span className="material-symbols-outlined text-on-surface-variant" style={{ fontSize: '1.25rem' }}>
              search
            </span>
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Name, code, village or phone"
              className="bg-transparent text-body-md text-on-surface placeholder:text-on-surface-variant focus:outline-none w-56"
            />
          </div>
        }
      />

      {farmers.isLoading ? (
        <LoadingState label="Loading farmers" />
      ) : farmers.error ? (
        <ErrorState message={farmers.error} onRetry={farmers.reload} />
      ) : all.length === 0 ? (
        <EmptyState
          title="No farmers registered"
          description="Farmer accounts are created by the market registry. Once registered, they appear here."
          icon="group_off"
        />
      ) : visible.length === 0 ? (
        <EmptyState
          title="No farmers match that search"
          description={`None of the ${all.length} registered farmers match "${search}".`}
          icon="search_off"
          action={
            <button
              type="button"
              onClick={() => setSearch('')}
              className="mt-2 inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-outline-variant text-label-lg font-semibold text-on-surface hover:bg-surface-container-high transition-colors"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                restart_alt
              </span>
              Clear search
            </button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
          {visible.map((farmer) => (
            <div
              key={farmer.id}
              className="bg-surface-container-lowest rounded-xl shadow-sm p-4 flex flex-col gap-3"
            >
              <div className="flex items-start gap-3">
                <div className="w-14 h-14 rounded-xl bg-primary-container flex items-center justify-center shrink-0">
                  <span className="material-symbols-outlined text-on-primary-container" style={{ fontSize: '2rem' }}>
                    person
                  </span>
                </div>
                <div className="min-w-0 flex-1">
                  <div className="text-headline-sm font-bold text-on-surface">{farmer.name}</div>
                  <div className="flex items-center gap-1 mt-0.5 flex-wrap">
                    <span className="text-label-sm text-primary font-mono font-semibold">
                      {farmer.farmerCode ?? 'No code'}
                    </span>
                    <span
                      className={`px-1 py-0.5 text-label-sm rounded ${
                        farmer.status === 'ACTIVE'
                          ? 'bg-primary-container text-on-primary-container'
                          : 'bg-surface-container-highest text-on-surface-variant'
                      }`}
                    >
                      {farmer.status ?? 'Unknown'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex flex-col gap-0.5 text-body-sm text-on-surface-variant">
                <div className="flex items-center gap-1">
                  <span className="material-symbols-outlined" style={{ fontSize: '0.875rem' }}>
                    location_on
                  </span>
                  {farmer.village || 'No village recorded'}
                </div>
                <div className="flex items-center gap-1">
                  <span className="material-symbols-outlined" style={{ fontSize: '0.875rem' }}>
                    phone
                  </span>
                  {farmer.phone || 'No phone recorded'}
                </div>
                <div className="flex items-center gap-1 truncate">
                  <span className="material-symbols-outlined" style={{ fontSize: '0.875rem' }}>
                    home
                  </span>
                  {farmer.address || 'No address recorded'}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <p className="text-body-sm text-on-surface-variant">
        This directory is read-only. Farmer registration and corrections are handled by the market
        registry, and a farmer's own passbook is visible to that farmer at{' '}
        <span className="font-semibold">My Passbook</span> after they sign in.
      </p>
    </div>
  );
}
