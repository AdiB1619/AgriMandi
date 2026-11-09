import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '../../components/ui/DataTable';
import { FilterBar, FilterField, filterControlClass } from '../../components/ui/FilterBar';
import { EmptyState } from '../../components/ui/EmptyState';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { PaymentStatusPill } from '../../components/ui/StatusPills';
import { StatCard } from '../../components/ui/StatCard';
import { useAsyncData } from '../../hooks/useAsyncData';
import {
  PERIOD_OPTIONS,
  referenceApi,
  reportsApi,
  type BuyerActivity,
  type FarmerActivity,
  type PeriodFilter,
  type ReportPeriod,
} from '../../services/reportsApi';
import { formatCurrency, formatDate, formatNumber } from '../../utils/format';

type SectionKey = 'transactions' | 'payments' | 'assets' | 'auctions' | 'buyers' | 'farmers';

const SECTIONS: { key: SectionKey; label: string }[] = [
  { key: 'transactions', label: 'Transaction Overview' },
  { key: 'payments', label: 'Payment Collection' },
  { key: 'assets', label: 'Outstanding Payments' },
  { key: 'auctions', label: 'Returnable Asset Ledger' },
  { key: 'buyers', label: 'Buyer Activity' },
  { key: 'farmers', label: 'Farmer Activity' },
];

const SECTION_ICON: Record<SectionKey, string> = {
  transactions: 'receipt_long',
  payments: 'payments',
  assets: 'pending_actions',
  auctions: 'gavel',
  buyers: 'storefront',
  farmers: 'group',
};

export function Reports() {
  const navigate = useNavigate();

  const [period, setPeriod] = useState<ReportPeriod>('CURRENT_MONTH');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [buyerId, setBuyerId] = useState('');
  const [farmerId, setFarmerId] = useState('');
  const [section, setSection] = useState<SectionKey>('transactions');

  const buyers = useAsyncData(() => referenceApi.buyers(), []);
  const farmers = useAsyncData(() => referenceApi.farmers(), []);

  const customRangeIncomplete = period === 'CUSTOM' && (from === '' || to === '');
  const rangeComplete = !customRangeIncomplete;

  const filter = useMemo<PeriodFilter>(
    () => ({ period, from, to }),
    [period, from, to],
  );

  const transactions = useAsyncData(
    () => (rangeComplete ? reportsApi.transactionSummary(filter) : Promise.resolve(null)),
    [filter, rangeComplete],
  );
  const payments = useAsyncData(
    () => (rangeComplete ? reportsApi.paymentSummary(filter) : Promise.resolve(null)),
    [filter, rangeComplete],
  );
  const assets = useAsyncData(
    () => (rangeComplete ? reportsApi.assetSummary(filter) : Promise.resolve(null)),
    [filter, rangeComplete],
  );
  const auctions = useAsyncData(
    () => (rangeComplete ? reportsApi.auctionSummary(filter) : Promise.resolve(null)),
    [filter, rangeComplete],
  );
  const buyerReport = useAsyncData(
    () => (rangeComplete ? reportsApi.buyerSummary({ ...filter, buyerId }) : Promise.resolve(null)),
    [filter, buyerId, rangeComplete],
  );
  const farmerReport = useAsyncData(
    () => (rangeComplete ? reportsApi.farmerSummary({ ...filter, farmerId }) : Promise.resolve(null)),
    [filter, farmerId, rangeComplete],
  );

  const isLoading =
    transactions.isLoading || payments.isLoading || assets.isLoading || auctions.isLoading ||
    buyerReport.isLoading || farmerReport.isLoading;

  const error = transactions.error || payments.error || assets.error || auctions.error ||
    buyerReport.error || farmerReport.error;

  function resetFilters() {
    setPeriod('CURRENT_MONTH');
    setFrom('');
    setTo('');
    setBuyerId('');
    setFarmerId('');
    setSection('transactions');
  }

  const rangeLabel = transactions.data
    ? `${formatDate(transactions.data.range.from)} – ${formatDate(transactions.data.range.to)}`
    : '—';

  const buyerColumns: DataTableColumn<BuyerActivity>[] = [
    {
      key: 'buyer',
      header: 'Buyer',
      render: (row) => (
        <div className="flex flex-col">
          <span className="font-semibold text-on-surface">{row.buyerName}</span>
          {row.businessName && (
            <span className="text-body-sm text-on-surface-variant">{row.businessName}</span>
          )}
        </div>
      ),
    },
    { key: 'purchases', header: 'Purchases', align: 'right', render: (row) => formatNumber(row.purchases) },
    {
      key: 'value',
      header: 'Purchase value',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatCurrency(row.totalPurchaseValue)}</span>,
    },
    {
      key: 'quantity',
      header: 'Quantity',
      align: 'right',
      render: (row) => formatNumber(row.totalQuantity),
      secondary: true,
    },
    {
      key: 'outstanding',
      header: 'Outstanding',
      align: 'right',
      render: (row) => (
        <span className="text-numeric-tabular-md text-error">{formatCurrency(row.outstandingPayment)}</span>
      ),
    },
    {
      key: 'assets',
      header: 'Assets outstanding',
      align: 'right',
      render: (row) => formatNumber(row.outstandingAssetRecords),
      secondary: true,
    },
  ];

  const farmerColumns: DataTableColumn<FarmerActivity>[] = [
    { key: 'farmer', header: 'Farmer', render: (row) => <span className="font-semibold">{row.farmerName}</span> },
    { key: 'lots', header: 'Lots', align: 'right', render: (row) => formatNumber(row.lots) },
    {
      key: 'quantity',
      header: 'Quantity',
      align: 'right',
      render: (row) => formatNumber(row.totalQuantity),
      secondary: true,
    },
    {
      key: 'transactions',
      header: 'Transactions',
      align: 'right',
      render: (row) => formatNumber(row.transactions),
    },
    {
      key: 'value',
      header: 'Transaction value',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatCurrency(row.totalTransactionValue)}</span>,
    },
    {
      key: 'outstanding',
      header: 'Payment status',
      align: 'right',
      render: (row) => (
        <span className="text-numeric-tabular-md text-error">{formatCurrency(row.outstandingPayment)}</span>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-4 py-4">
      <PageHeader
        title="Reports & Analytics"
        subtitle={`Market-wide operational reporting · ${rangeLabel}`}
        icon="analytics"
      />

      <FilterBar onReset={resetFilters}>
        <FilterField label="Date range" className="w-44">
          <select
            value={period}
            onChange={(event) => setPeriod(event.target.value as ReportPeriod)}
            className={filterControlClass}
          >
            {PERIOD_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </FilterField>

        {period === 'CUSTOM' && (
          <>
            <FilterField label="From" className="w-36">
              <input
                type="date"
                value={from}
                max={to || undefined}
                onChange={(event) => setFrom(event.target.value)}
                className={filterControlClass}
              />
            </FilterField>
            <FilterField label="To" className="w-36">
              <input
                type="date"
                value={to}
                min={from || undefined}
                onChange={(event) => setTo(event.target.value)}
                className={filterControlClass}
              />
            </FilterField>
          </>
        )}

        <FilterField label="Buyer" className="w-44">
          <select value={buyerId} onChange={(event) => setBuyerId(event.target.value)} className={filterControlClass}>
            <option value="">All buyers</option>
            {(buyers.data ?? []).map((buyer) => (
              <option key={buyer.id} value={buyer.id}>
                {buyer.name}
              </option>
            ))}
          </select>
        </FilterField>

        <FilterField label="Farmer" className="w-44">
          <select value={farmerId} onChange={(event) => setFarmerId(event.target.value)} className={filterControlClass}>
            <option value="">All farmers</option>
            {(farmers.data ?? []).map((farmer) => (
              <option key={farmer.id} value={farmer.id}>
                {farmer.name}
              </option>
            ))}
          </select>
        </FilterField>
      </FilterBar>

      <div className="flex gap-1 overflow-x-auto bg-surface-container-lowest rounded-xl shadow-sm p-1">
        {SECTIONS.map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={() => setSection(item.key)}
            className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-label-lg font-semibold whitespace-nowrap transition-colors ${
              section === item.key
                ? 'bg-primary-container text-on-primary'
                : 'text-on-surface-variant hover:bg-surface-container-high'
            }`}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
              {SECTION_ICON[item.key]}
            </span>
            {item.label}
          </button>
        ))}
      </div>

      {customRangeIncomplete ? (
        <div className="bg-surface-container-lowest rounded-xl p-4 flex items-center gap-2">
          <span className="material-symbols-outlined text-on-surface-variant" style={{ fontSize: '1.25rem' }}>
            info
          </span>
          <span className="text-body-md text-on-surface-variant">
            Select both a start and an end date to run a custom range.
          </span>
        </div>
      ) : isLoading ? (
        <LoadingState label="Computing reports" />
      ) : error ? (
        <ErrorState message={error} onRetry={transactions.reload} />
      ) : (
        <>
          {section === 'transactions' && transactions.data && (
            <div className="flex flex-col gap-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
                <StatCard title="Transactions" icon="receipt_long" value={formatNumber(transactions.data.aggregate.totalTransactions)} />
                <StatCard
                  title="Gross sale value"
                  icon="currency_rupee"
                  value={formatCurrency(transactions.data.aggregate.grossSaleValue)}
                />
                <StatCard
                  title="Quantity sold"
                  icon="scale"
                  value={formatNumber(transactions.data.aggregate.totalQuantity)}
                />
                <StatCard
                  title="Average sale rate"
                  icon="functions"
                  value={formatNumber(transactions.data.aggregate.averageSaleRate)}
                />
              </div>

              <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
                <div className="px-3 py-2 border-b border-surface-container-high">
                  <h2 className="text-headline-sm font-semibold text-on-surface">Transaction status breakdown</h2>
                </div>
                {transactions.data.statusBreakdown.length === 0 ? (
                  <EmptyState
                    title="No transactions in this period"
                    description="Complete a physical auction and confirm the winner to generate a transaction."
                    icon="receipt_long"
                  />
                ) : (
                  <DataTable
                    columns={[
                      { key: 'status', header: 'Status', render: (row: { status: string; count: number }) => <PaymentStatusPill status={row.status} /> },
                      { key: 'count', header: 'Transactions', align: 'right', render: (row: { count: number }) => formatNumber(row.count) },
                    ]}
                    rows={transactions.data.statusBreakdown}
                    rowKey={(row) => row.status}
                    caption="Transaction status breakdown"
                  />
                )}
              </div>
            </div>
          )}

          {section === 'payments' && payments.data && (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
              <StatCard title="Total billed" icon="request_quote" value={formatCurrency(payments.data.aggregate.totalBilled)} />
              <StatCard title="Total received" icon="savings" value={formatCurrency(payments.data.aggregate.totalReceived)} iconColor="text-secondary" />
              <StatCard title="Total outstanding" icon="pending_actions" value={formatCurrency(payments.data.aggregate.totalOutstanding)} iconColor="text-error" />
              <StatCard title="Partially paid" icon="donut_small" value={formatNumber(payments.data.aggregate.partiallyPaidCount)} />
              <StatCard title="Settled" icon="task_alt" value={formatNumber(payments.data.aggregate.settledCount)} iconColor="text-secondary" />
              <StatCard title="Transactions billed" icon="receipt_long" value={formatNumber(payments.data.aggregate.totalTransactions)} />
            </div>
          )}

          {section === 'assets' && (
            <div className="flex flex-col gap-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
                <StatCard
                  title="Outstanding payments"
                  icon="pending_actions"
                  iconColor="text-error"
                  value={formatCurrency(transactions.data?.totalOutstandingPayment)}
                  subValue={
                    <span className="text-body-sm text-on-surface-variant">
                      Billed {formatCurrency(payments.data?.aggregate.totalBilled)} · Received{' '}
                      {formatCurrency(payments.data?.aggregate.totalReceived)}
                    </span>
                  }
                />
                <StatCard title="Partially paid" icon="donut_small" value={formatNumber(payments.data?.aggregate.partiallyPaidCount)} />
                <StatCard title="Settled" icon="task_alt" iconColor="text-secondary" value={formatNumber(payments.data?.aggregate.settledCount)} />
                <StatCard
                  title="Assets outstanding"
                  icon="local_shipping"
                  value={formatNumber(assets.data?.aggregate.totalOutstanding)}
                />
              </div>

              <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
                <div className="px-3 py-2 border-b border-surface-container-high flex items-center justify-between">
                  <h2 className="text-headline-sm font-semibold text-on-surface">Transactions awaiting settlement</h2>
                  <button
                    type="button"
                    onClick={() => navigate('/payment-ledger')}
                    className="text-label-lg font-semibold text-primary hover:underline"
                  >
                    Open payment ledger →
                  </button>
                </div>
                {(transactions.data?.statusBreakdown.length ?? 0) === 0 ? (
                  <EmptyState
                    title="Nothing outstanding"
                    description="Every transaction in this period is settled."
                    icon="task_alt"
                  />
                ) : (
                  <DataTable
                    columns={[
                      {
                        key: 'status',
                        header: 'Status',
                        render: (row: { status: string; count: number }) => <PaymentStatusPill status={row.status} />,
                      },
                      {
                        key: 'count',
                        header: 'Transactions',
                        align: 'right',
                        render: (row: { count: number }) => formatNumber(row.count),
                      },
                    ]}
                    rows={transactions.data?.statusBreakdown ?? []}
                    rowKey={(row) => row.status}
                    caption="Transactions awaiting settlement"
                  />
                )}
              </div>
            </div>
          )}

          {section === 'auctions' && (
            <div className="flex flex-col gap-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
                <StatCard title="Lots" icon="inventory_2" value={formatNumber(auctions.data?.aggregate.totalLots)} />
                <StatCard title="Auctions" icon="gavel" value={formatNumber(auctions.data?.aggregate.totalAuctions)} />
                <StatCard title="Completed auctions" icon="task_alt" iconColor="text-secondary" value={formatNumber(auctions.data?.aggregate.completedAuctions)} />
                <StatCard title="Active auctions" icon="sensors" value={formatNumber(auctions.data?.aggregate.activeAuctions)} />
                <StatCard title="Tie cases" icon="call_split" iconColor="text-error" value={formatNumber(auctions.data?.aggregate.tieCases)} />
                <StatCard title="Transactions generated" icon="receipt_long" value={formatNumber(auctions.data?.aggregate.transactionsGenerated)} />
                <StatCard title="Quantity auctioned" icon="scale" value={formatNumber(auctions.data?.aggregate.totalQuantityAuctioned)} />
              </div>

              <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
                <div className="px-3 py-2 border-b border-surface-container-high flex items-center justify-between">
                  <h2 className="text-headline-sm font-semibold text-on-surface">Returnable asset movements</h2>
                  <button
                    type="button"
                    onClick={() => navigate('/returnable-asset-ledger')}
                    className="text-label-lg font-semibold text-primary hover:underline"
                  >
                    Open asset ledger →
                  </button>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 divide-y sm:divide-y-0 sm:divide-x divide-surface-container-high">
                  <div className="p-4">
                    <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">Issued</span>
                    <p className="text-numeric-currency-lg text-on-surface">
                      {formatNumber(assets.data?.aggregate.totalIssued)}
                    </p>
                  </div>
                  <div className="p-4">
                    <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">Returned</span>
                    <p className="text-numeric-currency-lg text-secondary">
                      {formatNumber(assets.data?.aggregate.totalReturned)}
                    </p>
                  </div>
                  <div className="p-4">
                    <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">Outstanding</span>
                    <p className="text-numeric-currency-lg text-error">
                      {formatNumber(assets.data?.aggregate.totalOutstanding)}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {section === 'buyers' && (
            <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
              <div className="px-3 py-2 border-b border-surface-container-high">
                <h2 className="text-headline-sm font-semibold text-on-surface">Buyer activity</h2>
              </div>
              {(buyerReport.data?.buyers.length ?? 0) === 0 ? (
                <EmptyState
                  title="No buyer purchases in this period"
                  description="Buyer totals are derived from confirmed transactions only."
                  icon="storefront"
                />
              ) : (
                <DataTable
                  columns={buyerColumns}
                  rows={buyerReport.data?.buyers ?? []}
                  rowKey={(row) => row.buyerId}
                  caption="Buyer activity"
                />
              )}
            </div>
          )}

          {section === 'farmers' && (
            <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
              <div className="px-3 py-2 border-b border-surface-container-high">
                <h2 className="text-headline-sm font-semibold text-on-surface">Farmer activity</h2>
              </div>
              {(farmerReport.data?.farmers.length ?? 0) === 0 ? (
                <EmptyState
                  title="No farmer activity in this period"
                  description="Farmer totals are derived from lots and confirmed transactions in this period."
                  icon="group"
                />
              ) : (
                <DataTable
                  columns={farmerColumns}
                  rows={farmerReport.data?.farmers ?? []}
                  rowKey={(row) => row.farmerId}
                  caption="Farmer activity"
                />
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
