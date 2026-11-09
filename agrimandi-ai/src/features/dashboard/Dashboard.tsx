import { Link, useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { reportsApi } from '../../services/reportsApi';
import { useAsyncData } from '../../hooks/useAsyncData';
import { useAuth } from '../auth/AuthContext';
import { PageHeader } from '../../components/ui/PageHeader';
import { StatCard } from '../../components/ui/StatCard';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { EmptyState } from '../../components/ui/EmptyState';
import { DataTable } from '../../components/ui/DataTable';
import { LotStatusPill, PaymentStatusPill } from '../../components/ui/StatusPills';
import { formatCurrency, formatNumber, formatDateTime } from '../../utils/format';
import type { PaymentLedgerRow } from '../../services/reportsApi';

interface LotSummary {
  id: string;
  lotNumber: string;
  status: string;
  quantity: number | null;
  quantityUnit: string | null;
  commodity?: { name?: string } | null;
  farmer?: { name?: string } | null;
  createdAt?: string;
}

/** Real lot statuses that make an agent's day; anything else falls into the detail table. */
const ACTIONABLE_STATUSES = ['READY_FOR_AUCTION', 'AUCTION_IN_PROGRESS', 'TIE_REQUIRES_AGENT_DECISION', 'WINNER_PENDING_CONFIRMATION'];

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const lots = useAsyncData<LotSummary[]>(() => api.get<LotSummary[]>('/api/lots'), []);
  const payments = useAsyncData(
    () => reportsApi.paymentSummary({ period: 'TODAY' }),
    [],
  );
  const overview = useAsyncData(
    () => reportsApi.transactionSummary({ period: 'CURRENT_MONTH' }),
    [],
  );
  const ledger = useAsyncData(() => reportsApi.paymentLedger({ period: 'CURRENT_MONTH', page: 0, size: 5 }), []);

  const reloadAll = () => {
    lots.reload();
    payments.reload();
    overview.reload();
    ledger.reload();
  };

  const lotList = lots.data ?? [];
  const actionable = lotList.filter((lot) => ACTIONABLE_STATUSES.includes(lot.status));
  const awaitingWeighment = lotList.filter((lot) => lot.status === 'PENDING_WEIGHMENT').length;
  const settled = lotList.filter((lot) => lot.status === 'TRANSACTION_CREATED').length;
  const todayPayments = payments.data?.aggregate;
  const month = overview.data;

  return (
    <div className="flex flex-col w-full pb-space-2xl gap-space-lg">
      <PageHeader
        title={`Welcome, ${user?.username ?? 'Agent'}`}
        subtitle="Live operational position for your market, straight from the APMC ledger."
        icon="dashboard"
        actions={
          <button
            type="button"
            onClick={reloadAll}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-outline-variant text-label-lg font-semibold text-on-surface hover:bg-surface-container-high transition-colors"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
              refresh
            </span>
            Refresh
          </button>
        }
      />

      {lots.isLoading ? (
        <LoadingState label="Loading your market position" />
      ) : lots.error ? (
        <ErrorState message={lots.error} onRetry={lots.reload} />
      ) : (
        <>
          {/* Quick actions — every tile below is a real destination. */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-space-sm">
            <QuickAction
              to="/lots-management?create=1"
              icon="add_box"
              label="Inward Lot"
              hint="Record a new lot and weighment"
            />
            <QuickAction
              to="/auction-desk"
              icon="gavel"
              label="Auction Desk"
              hint={
                actionable.length > 0
                  ? `${actionable.length} lot${actionable.length === 1 ? '' : 's'} need attention`
                  : 'Start or settle an auction'
              }
            />
            <QuickAction
              to="/payment-ledger"
              icon="point_of_sale"
              label="Payment Ledger"
              hint="Record and review settlements"
            />
          </div>

          {/* Operational KPIs, all derived from backend responses. */}
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-space-md">
            <StatCard
              title="Lots In Market"
              icon="inventory_2"
              value={<span className="text-headline-xl font-bold text-on-surface">{formatNumber(lotList.length)}</span>}
              footer={
                <>
                  <span>{settled} settled</span>
                  <span className="text-secondary font-semibold">{actionable.length} in auction flow</span>
                  <span>{awaitingWeighment} awaiting weighment</span>
                </>
              }
            />

            <StatCard
              title="Today’s Payments"
              icon="currency_rupee"
              value={
                payments.isLoading ? (
                  <span className="text-body-sm text-on-surface-variant italic">Loading…</span>
                ) : payments.error ? (
                  <span className="text-body-sm text-error font-semibold">Unavailable</span>
                ) : (
                  <span className="text-headline-xl font-bold text-on-surface">
                    {formatCurrency(todayPayments?.totalReceived ?? 0)}
                  </span>
                )
              }
              footer={
                payments.error ? (
                  <button
                    type="button"
                    onClick={payments.reload}
                    className="text-error font-semibold underline"
                  >
                    Retry
                  </button>
                ) : (
                  <>
                    <span>Billed {formatCurrency(todayPayments?.totalBilled ?? 0)}</span>
                    <span className="text-error font-semibold">
                      Outstanding {formatCurrency(todayPayments?.totalOutstanding ?? 0)}
                    </span>
                  </>
                )
              }
            />

            <StatCard
              title="Month-to-Date Value"
              icon="trending_up"
              value={
                overview.isLoading ? (
                  <span className="text-body-sm text-on-surface-variant italic">Loading…</span>
                ) : overview.error ? (
                  <span className="text-body-sm text-error font-semibold">Unavailable</span>
                ) : (
                  <span className="text-headline-xl font-bold text-on-surface">
                    {formatCurrency(month?.aggregate.netSaleValue ?? 0)}
                  </span>
                )
              }
              footer={
                overview.error ? (
                  <button
                    type="button"
                    onClick={overview.reload}
                    className="text-error font-semibold underline"
                  >
                    Retry
                  </button>
                ) : (
                  <span>
                    {formatNumber(month?.aggregate.totalTransactions ?? 0)} transactions this month
                  </span>
                )
              }
            />

            <StatCard
              title="Outstanding Payments"
              icon="pending_actions"
              value={
                overview.isLoading ? (
                  <span className="text-body-sm text-on-surface-variant italic">Loading…</span>
                ) : overview.error ? (
                  <span className="text-body-sm text-error font-semibold">Unavailable</span>
                ) : (
                  <span className="text-headline-xl font-bold text-error">
                    {formatCurrency(month?.totalOutstandingPayment ?? 0)}
                  </span>
                )
              }
              footer={
                overview.error ? (
                  <button
                    type="button"
                    onClick={overview.reload}
                    className="text-error font-semibold underline"
                  >
                    Retry
                  </button>
                ) : (
                  <span>Market-wide, month to date</span>
                )
              }
            />
          </div>

          {/* Needs attention */}
          {actionable.length > 0 ? (
            <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
              <div className="px-3 py-2 border-b border-surface-container-high flex items-center gap-2">
                <span className="material-symbols-outlined text-warning" style={{ fontSize: '1.25rem' }}>
                  priority_high
                </span>
                <h2 className="text-headline-sm font-semibold text-on-surface">Lots needing action</h2>
              </div>
              <DataTable
                rows={actionable}
                rowKey={(row) => row.id}
                caption="Lots that require an agent action"
                columns={[
                  { key: 'lot', header: 'Lot', render: (row) => <span className="font-semibold">{row.lotNumber}</span> },
                  { key: 'commodity', header: 'Commodity', render: (row) => row.commodity?.name ?? '—', secondary: true },
                  { key: 'farmer', header: 'Farmer', render: (row) => row.farmer?.name ?? '—' },
                  {
                    key: 'qty',
                    header: 'Quantity',
                    align: 'right',
                    render: (row) => `${formatNumber(row.quantity)} ${row.quantityUnit ?? ''}`.trim(),
                  },
                  { key: 'status', header: 'Status', render: (row) => <LotStatusPill status={row.status} /> },
                ]}
                onRowClick={() => undefined}
              />
              <div className="px-3 py-2 border-t border-surface-container-high">
                <Link
                  to="/auction-desk"
                  className="inline-flex items-center gap-1 text-label-lg font-semibold text-primary hover:underline"
                >
                  Open Auction Desk
                  <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                    arrow_forward
                  </span>
                </Link>
              </div>
            </div>
          ) : (
            <EmptyState
              title="No lots are waiting on you"
              description="When a lot is inwarded and weighed it will appear here until its auction is settled."
              icon="task_alt"
            />
          )}

          {/* Recent settlements */}
          <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
            <div className="px-3 py-2 border-b border-surface-container-high flex items-center justify-between">
              <h2 className="text-headline-sm font-semibold text-on-surface">
                Recent settlements (this month)
              </h2>
              <Link
                to="/transactions-billing"
                className="text-label-lg font-semibold text-primary hover:underline"
              >
                View all
              </Link>
            </div>

            {ledger.isLoading ? (
              <div className="p-4">
                <LoadingState label="Loading settlements" />
              </div>
            ) : ledger.error ? (
              <div className="p-4">
                <ErrorState message={ledger.error} onRetry={ledger.reload} />
              </div>
            ) : (ledger.data?.content.length ?? 0) === 0 ? (
              <div className="p-4">
                <EmptyState
                  title="No transactions this month"
                  description="Completed auctions appear here once a winner is confirmed."
                  icon="receipt_long"
                />
              </div>
            ) : (
              <DataTable<PaymentLedgerRow>
                rows={ledger.data?.content ?? []}
                rowKey={(row) => row.transactionId}
                caption="Recent transactions and their settlement position"
                columns={[
                  {
                    key: 'txn',
                    header: 'Transaction',
                    render: (row) => <span className="font-semibold">{row.transactionNumber}</span>,
                  },
                  { key: 'farmer', header: 'Farmer', render: (row) => row.farmerName, secondary: true },
                  { key: 'buyer', header: 'Buyer', render: (row) => row.buyerName },
                  {
                    key: 'net',
                    header: 'Net Amount',
                    align: 'right',
                    render: (row) => formatCurrency(row.totalAmount),
                  },
                  {
                    key: 'outstanding',
                    header: 'Outstanding',
                    align: 'right',
                    render: (row) => formatCurrency(row.outstandingAmount),
                  },
                  { key: 'status', header: 'Status', render: (row) => <PaymentStatusPill status={row.paymentStatus} /> },
                  {
                    key: 'when',
                    header: 'Date',
                    align: 'right',
                    secondary: true,
                    render: (row) => formatDateTime(row.transactionDate),
                  },
                ]}
                onRowClick={(row) => navigate(`/transactions-billing/${row.transactionId}`)}
              />
            )}
          </div>
        </>
      )}
    </div>
  );
}

function QuickAction({
  to,
  icon,
  label,
  hint,
}: {
  to: string;
  icon: string;
  label: string;
  hint: string;
}) {
  return (
    <Link
      to={to}
      className="flex flex-col gap-1 px-space-md py-space-sm bg-surface-container-lowest rounded-lg shadow-sm hover:bg-surface-container-high transition-all group"
    >
      <div className="flex items-center gap-space-xs">
        <span className="material-symbols-outlined text-space-lg text-primary group-hover:scale-110 transition-transform">
          {icon}
        </span>
        <span className="font-label-lg text-label-lg font-semibold text-on-surface truncate">{label}</span>
      </div>
      <span className="text-body-sm text-on-surface-variant truncate">{hint}</span>
    </Link>
  );
}
