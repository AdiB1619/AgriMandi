import { useState } from 'react';
import { api } from '../../services/api';
import { useAsyncData } from '../../hooks/useAsyncData';
import { PageHeader } from '../../components/ui/PageHeader';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { EmptyState } from '../../components/ui/EmptyState';
import { DataTable } from '../../components/ui/DataTable';

interface Buyer {
  id: string;
  buyerCode: string | null;
  name: string;
  businessName: string | null;
  phone: string | null;
  address: string | null;
  licenseReference: string | null;
  status: string | null;
}

/**
 * Read-only directory of the licensed buyers in this market, from `GET /api/buyers`.
 *
 * <p>No register control is offered because the market exposes no create-buyer endpoint, and no
 * per-buyer drill-down is offered because a buyer's purchases are visible to that buyer only.
 */
export function BuyersDirectory() {
  const buyers = useAsyncData<Buyer[]>(() => api.get<Buyer[]>('/api/buyers'), []);
  const [search, setSearch] = useState('');

  const all = buyers.data ?? [];
  const term = search.trim().toLowerCase();
  const visible = term
    ? all.filter((buyer) =>
        [buyer.name, buyer.buyerCode, buyer.businessName, buyer.licenseReference, buyer.phone]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(term)),
      )
    : all;

  return (
    <div className="flex flex-col w-full pb-space-2xl gap-space-lg">
      <PageHeader
        title="Buyers Directory"
        subtitle="Licensed APMC traders registered with this market"
        icon="storefront"
        badge={
          <span className="px-2 py-0.5 bg-primary text-on-primary text-label-sm rounded uppercase">
            {all.length} licensed
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
              placeholder="Name, code, business or licence"
              className="bg-transparent text-body-md text-on-surface placeholder:text-on-surface-variant focus:outline-none w-56"
            />
          </div>
        }
      />

      <div className="bg-surface-container-lowest rounded-xl shadow-sm">
        {buyers.isLoading ? (
          <div className="p-4">
            <LoadingState label="Loading buyers" />
          </div>
        ) : buyers.error ? (
          <div className="p-4">
            <ErrorState message={buyers.error} onRetry={buyers.reload} />
          </div>
        ) : all.length === 0 ? (
          <div className="p-4">
            <EmptyState
              title="No buyers registered"
              description="Buyer accounts are created by the market registry. Once licensed, they appear here."
              icon="storefront"
            />
          </div>
        ) : visible.length === 0 ? (
          <div className="p-4">
            <EmptyState
              title="No buyers match that search"
              description={`None of the ${all.length} registered buyers match "${search}".`}
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
          </div>
        ) : (
          <DataTable<Buyer>
            rows={visible}
            rowKey={(row) => row.id}
            caption="Licensed buyers registered in this market"
            columns={[
              {
                key: 'code',
                header: 'Code / Licence',
                render: (row) => (
                  <div className="min-w-0">
                    <div className="font-bold text-primary">{row.buyerCode ?? '—'}</div>
                    <div className="text-body-sm text-on-surface-variant">
                      {row.licenseReference || 'No licence reference'}
                    </div>
                  </div>
                ),
              },
              { key: 'name', header: 'Buyer', render: (row) => <span className="font-semibold">{row.name}</span> },
              {
                key: 'business',
                header: 'Business',
                secondary: true,
                render: (row) => row.businessName || '—',
              },
              {
                key: 'address',
                header: 'Address',
                secondary: true,
                render: (row) => (
                  <span className="truncate inline-block max-w-[16rem]">{row.address || '—'}</span>
                ),
              },
              { key: 'phone', header: 'Phone', secondary: true, render: (row) => row.phone || '—' },
              {
                key: 'status',
                header: 'Status',
                render: (row) => (
                  <span
                    className={`px-1 py-0.5 text-label-sm rounded ${
                      row.status === 'ACTIVE'
                        ? 'bg-primary-container text-on-primary-container'
                        : 'bg-surface-container-highest text-on-surface-variant'
                    }`}
                  >
                    {row.status ?? 'Unknown'}
                  </span>
                ),
              },
            ]}
          />
        )}
      </div>

      <p className="text-body-sm text-on-surface-variant">
        This directory is read-only. A buyer's own purchases are visible to that buyer at{' '}
        <span className="font-semibold">Purchase Statement</span> after they sign in.
      </p>
    </div>
  );
}
