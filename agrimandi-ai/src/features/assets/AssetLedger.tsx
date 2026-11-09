import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '../../components/ui/DataTable';
import { FilterBar, FilterField, filterControlClass } from '../../components/ui/FilterBar';
import { EmptyState } from '../../components/ui/EmptyState';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { Pagination } from '../../components/ui/Pagination';
import { AssetStatusPill } from '../../components/ui/StatusPills';
import { useAsyncData } from '../../hooks/useAsyncData';
import {
  PERIOD_OPTIONS,
  referenceApi,
  reportsApi,
  type AssetLedgerRow,
  type ReportPeriod,
} from '../../services/reportsApi';
import { formatDate, formatNumber } from '../../utils/format';

export function AssetLedger() {
  const navigate = useNavigate();

  const [period, setPeriod] = useState<ReportPeriod>('CURRENT_MONTH');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [buyerId, setBuyerId] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const buyers = useAsyncData(() => referenceApi.buyers(), []);

  const customRangeIncomplete = period === 'CUSTOM' && (from === '' || to === '');

  const ledgerFilter = useMemo(
    () => ({ period, from, to, buyerId, search, page, size }),
    [period, from, to, buyerId, search, page, size],
  );

  const ledger = useAsyncData(
    () =>
      customRangeIncomplete
        ? Promise.resolve({
            content: [] as AssetLedgerRow[],
            page: 0,
            size,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true,
          })
        : reportsApi.assetLedger(ledgerFilter),
    [ledgerFilter, customRangeIncomplete, size],
  );

  function resetFilters() {
    setPeriod('CURRENT_MONTH');
    setFrom('');
    setTo('');
    setBuyerId('');
    setSearchInput('');
    setSearch('');
    setPage(0);
  }

  const rows = ledger.data?.content ?? [];
  const totalIssued = rows.reduce((sum, row) => sum + Number(row.quantityIssued ?? 0), 0);
  const totalReturned = rows.reduce((sum, row) => sum + Number(row.quantityReturned ?? 0), 0);
  const totalOutstanding = rows.reduce((sum, row) => sum + Number(row.quantityOutstanding ?? 0), 0);

  const columns: DataTableColumn<AssetLedgerRow>[] = [
    {
      key: 'transaction',
      header: 'Transaction / Lot',
      render: (row) => (
        <div className="flex flex-col">
          <span className="font-semibold text-primary">{row.transactionNumber}</span>
          <span className="text-body-sm text-on-surface-variant">{row.lotNumber}</span>
        </div>
      ),
    },
    {
      key: 'buyer',
      header: 'Buyer',
      render: (row) => row.buyerName,
    },
    {
      key: 'assetType',
      header: 'Asset type',
      render: (row) => row.assetTypeName,
      secondary: true,
    },
    {
      key: 'issued',
      header: 'Issued',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatNumber(row.quantityIssued)}</span>,
    },
    {
      key: 'returned',
      header: 'Returned',
      align: 'right',
      render: (row) => <span className="text-numeric-tabular-md">{formatNumber(row.quantityReturned)}</span>,
    },
    {
      key: 'outstanding',
      header: 'Outstanding',
      align: 'right',
      render: (row) => (
        <span className="text-numeric-tabular-md text-error">
          {formatNumber(row.quantityOutstanding)}
        </span>
      ),
    },
    {
      key: 'issueDate',
      header: 'Issued on',
      render: (row) => <span className="whitespace-nowrap">{formatDate(row.issueDate)}</span>,
      secondary: true,
    },
    {
      key: 'returnDate',
      header: 'Last return',
      render: (row) => <span className="whitespace-nowrap">{formatDate(row.latestReturnDate)}</span>,
      secondary: true,
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <AssetStatusPill status={row.assetStatus} />,
    },
  ];

  return (
    <div className="flex flex-col gap-4 py-4">
      <PageHeader
        title="Returnable Asset Ledger"
        subtitle="Custody of returnable packaging issued against mandi transactions"
        icon="local_shipping"
        badge={
          ledger.data ? (
            <span className="px-2 py-0.5 bg-primary-container text-on-primary text-label-sm rounded uppercase">
              {ledger.data.totalElements} Records
            </span>
          ) : null
        }
      />

      <FilterBar onReset={resetFilters}>
        <FilterField label="Period" className="w-40">
          <select
            value={period}
            onChange={(event) => {
              setPeriod(event.target.value as ReportPeriod);
              setPage(0);
            }}
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
                onChange={(event) => {
                  setFrom(event.target.value);
                  setPage(0);
                }}
                className={filterControlClass}
              />
            </FilterField>
            <FilterField label="To" className="w-36">
              <input
                type="date"
                value={to}
                min={from || undefined}
                onChange={(event) => {
                  setTo(event.target.value);
                  setPage(0);
                }}
                className={filterControlClass}
              />
            </FilterField>
          </>
        )}

        <FilterField label="Buyer" className="w-44">
          <select
            value={buyerId}
            onChange={(event) => {
              setBuyerId(event.target.value);
              setPage(0);
            }}
            className={filterControlClass}
          >
            <option value="">All buyers</option>
            {(buyers.data ?? []).map((buyer) => (
              <option key={buyer.id} value={buyer.id}>
                {buyer.name}
              </option>
            ))}
          </select>
        </FilterField>

        <FilterField label="Search" className="w-56">
          <form
            onSubmit={(event) => {
              event.preventDefault();
              setSearch(searchInput);
              setPage(0);
            }}
            className="flex items-center gap-1 bg-surface-container-low rounded-lg border border-outline-variant px-2"
          >
            <span className="material-symbols-outlined text-on-surface-variant" style={{ fontSize: '1rem' }}>
              search
            </span>
            <input
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Transaction, lot, buyer, asset"
              className="bg-transparent py-1.5 text-body-md text-on-surface placeholder:text-on-surface-variant focus:outline-none w-full min-w-0"
            />
            <button type="submit" className="shrink-0 text-label-sm font-semibold text-primary hover:underline">
              Go
            </button>
          </form>
        </FilterField>
      </FilterBar>

      {customRangeIncomplete ? (
        <div className="bg-surface-container-lowest rounded-xl p-4 flex items-center gap-2">
          <span className="material-symbols-outlined text-on-surface-variant" style={{ fontSize: '1.25rem' }}>
            info
          </span>
          <span className="text-body-md text-on-surface-variant">
            Select both a start and an end date to run a custom range.
          </span>
        </div>
      ) : ledger.isLoading ? (
        <LoadingState label="Loading asset ledger" />
      ) : ledger.error ? (
        <ErrorState message={ledger.error} onRetry={ledger.reload} />
      ) : rows.length === 0 ? (
        <EmptyState
          title="No returnable asset movements for this selection"
          description="Assets appear here once a returnable asset type is issued against a completed transaction."
          icon="local_shipping"
        />
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="bg-surface-container-lowest rounded-xl shadow-sm p-4">
              <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">Issued (page)</span>
              <p className="text-numeric-currency-lg text-on-surface">{formatNumber(totalIssued)}</p>
            </div>
            <div className="bg-surface-container-lowest rounded-xl shadow-sm p-4">
              <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">Returned (page)</span>
              <p className="text-numeric-currency-lg text-secondary">{formatNumber(totalReturned)}</p>
            </div>
            <div className="bg-surface-container-lowest rounded-xl shadow-sm p-4">
              <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">
                Outstanding (page)
              </span>
              <p className="text-numeric-currency-lg text-error">{formatNumber(totalOutstanding)}</p>
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
            <DataTable
              columns={columns}
              rows={rows}
              rowKey={(row) => `${row.transactionId}-${row.assetTypeId}`}
              onRowClick={(row) => navigate(`/transactions-billing/${row.transactionId}`)}
              caption="Returnable asset ledger"
            />
            <div className="px-3 py-2 border-t border-surface-container-high">
              <Pagination
                page={ledger.data?.page ?? 0}
                size={size}
                totalElements={ledger.data?.totalElements ?? 0}
                totalPages={ledger.data?.totalPages ?? 0}
                onPageChange={setPage}
                onSizeChange={(next) => {
                  setSize(next);
                  setPage(0);
                }}
              />
            </div>
          </div>
        </>
      )}
    </div>
  );
}
