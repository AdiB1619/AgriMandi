import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { PageHeader } from '../../components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '../../components/ui/DataTable';
import { EmptyState } from '../../components/ui/EmptyState';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { StatCard } from '../../components/ui/StatCard';
import { ActionPill, PaymentStatusPill } from '../../components/ui/StatusPills';
import { useAsyncData } from '../../hooks/useAsyncData';
import {
  reportsApi,
  type AuditActivity,
  type BuyerActivity,
  type ExceptionCount,
  type FarmerActivity,
  type PaymentLedgerRow,
} from '../../services/reportsApi';
import { formatCurrency, formatDateTime, formatNumber } from '../../utils/format';

function SectionCard({ title, icon, children }: { title: string; icon: string; children: ReactNode }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
      <div className="px-3 py-2 border-b border-surface-container-high flex items-center gap-2">
        <span className="material-symbols-outlined text-primary" style={{ fontSize: '1.25rem' }}>
          {icon}
        </span>
        <h2 className="text-headline-sm font-semibold text-on-surface">{title}</h2>
      </div>
      {children}
    </div>
  );
}

export function AdminDashboard() {
  const oversight = useAsyncData(() => reportsApi.adminOversight(), []);
  const data = oversight.data;

  const recentTransactionColumns: DataTableColumn<PaymentLedgerRow>[] = [
    {
      key: 'transaction',
      header: 'Transaction',
      render: (row) => (
        <div className="flex flex-col">
          <span className="font-semibold text-primary">{row.transactionNumber}</span>
          <span className="text-body-sm text-on-surface-variant">{formatDateTime(row.transactionDate)}</span>
        </div>
      ),
    },
    { key: 'farmer', header: 'Farmer', render: (row) => row.farmerName, secondary: true },
    { key: 'buyer', header: 'Buyer', render: (row) => row.buyerName },
    {
      key: 'amount',
      header: 'Net amount',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatCurrency(row.totalAmount)}</span>,
    },
    {
      key: 'outstanding',
      header: 'Outstanding',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatCurrency(row.outstandingAmount)}</span>,
    },
    { key: 'status', header: 'Status', render: (row) => <PaymentStatusPill status={row.paymentStatus} /> },
  ];

  const topBuyerColumns: DataTableColumn<BuyerActivity>[] = [
    { key: 'buyer', header: 'Buyer', render: (row) => <span className="font-semibold">{row.buyerName}</span> },
    {
      key: 'value',
      header: 'Transaction value',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatCurrency(row.totalPurchaseValue)}</span>,
    },
    { key: 'purchases', header: 'Purchases', align: 'right', render: (row) => formatNumber(row.purchases) },
    {
      key: 'outstanding',
      header: 'Outstanding',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md text-error">{formatCurrency(row.outstandingPayment)}</span>,
      secondary: true,
    },
  ];

  const farmerColumns: DataTableColumn<FarmerActivity>[] = [
    { key: 'farmer', header: 'Farmer', render: (row) => <span className="font-semibold">{row.farmerName}</span> },
    { key: 'lots', header: 'Lots', align: 'right', render: (row) => formatNumber(row.lots) },
    {
      key: 'transactions',
      header: 'Transactions',
      align: 'right',
      render: (row) => formatNumber(row.transactions),
      secondary: true,
    },
    {
      key: 'value',
      header: 'Value',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatCurrency(row.totalTransactionValue)}</span>,
    },
  ];

  const auditColumns: DataTableColumn<AuditActivity>[] = [
    { key: 'action', header: 'Action', render: (row) => <ActionPill action={row.action} /> },
    { key: 'entity', header: 'Entity', render: (row) => row.entityType, secondary: true },
    { key: 'actor', header: 'Actor', render: (row) => row.actorUsername ?? 'System' },
    { key: 'role', header: 'Role', render: (row) => row.actorRole ?? '—', secondary: true },
    {
      key: 'timestamp',
      header: 'When',
      align: 'right',
      render: (row) => <span className="whitespace-nowrap">{formatDateTime(row.timestamp)}</span>,
    },
  ];

  const exceptionColumns: DataTableColumn<ExceptionCount>[] = [
    { key: 'label', header: 'Exception', render: (row) => row.label },
    {
      key: 'count',
      header: 'Records',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatNumber(row.count)}</span>,
    },
  ];

  return (
    <div className="flex flex-col gap-4 py-4">
      <PageHeader
        title="APMC Oversight Dashboard"
        subtitle={
          data
            ? `Market ${data.marketId.slice(0, 8)} · refreshed ${formatDateTime(data.generatedAt)}`
            : 'Live operational view of your market'
        }
        icon="monitoring"
        badge={
          <span className="px-2 py-0.5 bg-secondary-container text-on-secondary-container text-label-sm rounded uppercase">
            Admin
          </span>
        }
        actions={
          <>
            <Link
              to="/admin/reports"
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-primary-container text-on-primary text-label-lg font-semibold rounded-lg hover:bg-primary transition-colors"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                analytics
              </span>
              Reports
            </Link>
            <Link
              to="/admin/audit"
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-surface-container text-on-surface text-label-lg font-semibold rounded-lg border border-outline-variant hover:bg-surface-container-high transition-colors"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                history
              </span>
              Audit trail
            </Link>
          </>
        }
      />

      {oversight.isLoading ? (
        <LoadingState label="Loading oversight metrics" />
      ) : oversight.error ? (
        <ErrorState message={oversight.error} onRetry={oversight.reload} />
      ) : data ? (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
            <StatCard
              title="Today's lots"
              icon="inventory_2"
              value={formatNumber(data.todayLots.total)}
              footer={<span>Created since midnight</span>}
            />
            <StatCard
              title="Today's auction value"
              icon="gavel"
              value={formatCurrency(data.todayAuctionValue.total)}
              footer={<span>{formatNumber(data.todayTransactions.total)} transactions</span>}
            />
            <StatCard
              title="Payment collection today"
              icon="payments"
              iconColor="text-secondary"
              value={formatCurrency(data.todayPaymentCollection.total)}
              footer={<span>Payments dated today</span>}
            />
            <StatCard
              title="Outstanding payments"
              icon="pending_actions"
              iconColor="text-error"
              value={formatCurrency(data.totalOutstandingPayments.total)}
              footer={<span>All time, this market</span>}
            />
            <StatCard
              title="Returnable assets outstanding"
              icon="local_shipping"
              iconColor="text-error"
              value={formatNumber(data.outstandingAssets.totalOutstanding)}
              footer={<span>{formatNumber(data.outstandingAssets.totalIssued)} issued</span>}
            />
            <StatCard
              title="Active auctions"
              icon="sensors"
              value={formatNumber(data.activeAuctions.total)}
              footer={<span>Live on the trading floor</span>}
            />
            <StatCard
              title="Assets returned"
              icon="assignment_return"
              iconColor="text-secondary"
              value={formatNumber(data.outstandingAssets.totalReturned)}
              footer={<span>Custody released</span>}
            />
            <StatCard
              title="Open exceptions"
              icon="report_problem"
              iconColor="text-error"
              value={formatNumber(
                data.exceptions.reduce((sum, item) => sum + Number(item.count ?? 0), 0),
              )}
              footer={<span>Across {data.exceptions.length} categories</span>}
            />
          </div>

          <div className="grid grid-cols-1 xl:grid-cols-2 gap-3">
            <SectionCard title="Recent transactions" icon="receipt_long">
              {data.recentTransactions.length === 0 ? (
                <EmptyState
                  title="No transactions recorded"
                  description="Confirm an auction winner to generate the first transaction."
                  icon="receipt_long"
                />
              ) : (
                <DataTable
                  columns={recentTransactionColumns}
                  rows={data.recentTransactions}
                  rowKey={(row) => row.transactionId}
                  caption="Recent transactions"
                />
              )}
            </SectionCard>

            <SectionCard title="Recent audit activity" icon="history">
              {data.recentAuditActivity.length === 0 ? (
                <EmptyState
                  title="No audit activity yet"
                  description="Lot, weighment, auction, payment and asset events are recorded here as they happen."
                  icon="history"
                />
              ) : (
                <DataTable
                  columns={auditColumns}
                  rows={data.recentAuditActivity}
                  rowKey={(row) => row.id}
                  caption="Recent audit activity"
                />
              )}
            </SectionCard>

            <SectionCard title="Top buyers by transaction value (current month)" icon="storefront">
              {data.topBuyersByValue.length === 0 ? (
                <EmptyState
                  title="No buyer activity this month"
                  description="Rankings appear once transactions are confirmed in the current month."
                  icon="storefront"
                />
              ) : (
                <DataTable
                  columns={topBuyerColumns}
                  rows={data.topBuyersByValue}
                  rowKey={(row) => row.buyerId}
                  caption="Top buyers by transaction value"
                />
              )}
            </SectionCard>

            <SectionCard title="Farmer activity (current month)" icon="group">
              {data.farmerActivity.length === 0 ? (
                <EmptyState
                  title="No farmer activity this month"
                  description="Farmer totals combine lots created and transactions confirmed this month."
                  icon="group"
                />
              ) : (
                <DataTable
                  columns={farmerColumns}
                  rows={data.farmerActivity}
                  rowKey={(row) => row.farmerId}
                  caption="Farmer activity"
                />
              )}
            </SectionCard>

            <SectionCard title="Exceptions and issues" icon="report_problem">
              <DataTable
                columns={exceptionColumns}
                rows={data.exceptions}
                rowKey={(row) => row.label}
                caption="Exceptions and issues"
              />
            </SectionCard>
          </div>
        </>
      ) : null}
    </div>
  );
}
