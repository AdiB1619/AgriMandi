import { useMemo, useState } from 'react';
import { PageHeader } from '../../components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '../../components/ui/DataTable';
import { FilterBar, FilterField, filterControlClass } from '../../components/ui/FilterBar';
import { EmptyState } from '../../components/ui/EmptyState';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { Pagination } from '../../components/ui/Pagination';
import { ActionPill } from '../../components/ui/StatusPills';
import { useAsyncData } from '../../hooks/useAsyncData';
import {
  PERIOD_OPTIONS,
  auditApi,
  type AuditLogRow,
  type ReportPeriod,
} from '../../services/reportsApi';
import { formatDateTime } from '../../utils/format';

const ENTITY_TYPES = ['', 'LOT', 'AUCTION', 'BID', 'TRANSACTION', 'PAYMENT', 'ASSET_TRANSACTION'];

/** Read-only, admin-only view of the market audit trail. */
export function AuditTrail() {
  const [period, setPeriod] = useState<ReportPeriod>('CURRENT_MONTH');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [action, setAction] = useState('');
  const [entityType, setEntityType] = useState('');
  const [entityId, setEntityId] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);

  const customRangeIncomplete = period === 'CUSTOM' && (from === '' || to === '');

  const filter = useMemo(
    () => ({ period, from, to, action, entityType, entityId, page, size }),
    [period, from, to, action, entityType, entityId, page, size],
  );

  const audit = useAsyncData(
    () =>
      customRangeIncomplete
        ? Promise.resolve({
            content: [] as AuditLogRow[],
            page: 0,
            size,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true,
          })
        : auditApi.search(filter),
    [filter, customRangeIncomplete, size],
  );

  const actionOptions = useAsyncData(() => auditApi.actions(), []);

  function resetFilters() {
    setPeriod('CURRENT_MONTH');
    setFrom('');
    setTo('');
    setAction('');
    setEntityType('');
    setEntityId('');
    setPage(0);
  }

  const rows = audit.data?.content ?? [];

  const columns: DataTableColumn<AuditLogRow>[] = [
    {
      key: 'timestamp',
      header: 'Timestamp',
      render: (row) => <span className="whitespace-nowrap">{formatDateTime(row.timestamp)}</span>,
    },
    { key: 'action', header: 'Action', render: (row) => <ActionPill action={row.action} /> },
    { key: 'entityType', header: 'Entity', render: (row) => <span className="text-label-md">{row.entityType}</span> },
    {
      key: 'entityId',
      header: 'Entity ID',
      render: (row) => <span className="font-mono text-body-sm text-on-surface-variant">{row.entityId}</span>,
      secondary: true,
    },
    { key: 'actor', header: 'Actor', render: (row) => row.actorUsername ?? 'System' },
    {
      key: 'role',
      header: 'Role',
      render: (row) => (
        <span className="text-body-sm text-on-surface-variant">{row.actorRole ?? '—'}</span>
      ),
      secondary: true,
    },
    {
      key: 'market',
      header: 'Market',
      render: (row) => <span className="text-body-sm text-on-surface-variant">{row.marketCode ?? '—'}</span>,
      secondary: true,
    },
    {
      key: 'details',
      header: 'Details',
      render: (row) => (
        <span className="text-body-sm text-on-surface-variant break-words line-clamp-2">
          {row.newValue ?? '—'}
        </span>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-4 py-4">
      <PageHeader
        title="Audit Trail"
        subtitle="Immutable record of lot, weighment, auction, transaction, payment and asset events for your market"
        icon="history"
        badge={
          audit.data ? (
            <span className="px-2 py-0.5 bg-primary-container text-on-primary text-label-sm rounded uppercase">
              {audit.data.totalElements} Events
            </span>
          ) : null
        }
      />

      <FilterBar onReset={resetFilters}>
        <FilterField label="Date range" className="w-44">
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

        <FilterField label="Action" className="w-48">
          <select
            value={action}
            onChange={(event) => {
              setAction(event.target.value);
              setPage(0);
            }}
            className={filterControlClass}
          >
            <option value="">All actions</option>
            {(actionOptions.data ?? []).map((option) => (
              <option key={option} value={option}>
                {option.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </FilterField>

        <FilterField label="Entity type" className="w-48">
          <select
            value={entityType}
            onChange={(event) => {
              setEntityType(event.target.value);
              setPage(0);
            }}
            className={filterControlClass}
          >
            {ENTITY_TYPES.map((option) => (
              <option key={option || 'all'} value={option}>
                {option ? option.replace(/_/g, ' ') : 'All entities'}
              </option>
            ))}
          </select>
        </FilterField>

        <FilterField label="Entity ID" className="w-64">
          <input
            type="text"
            value={entityId}
            onChange={(event) => {
              setEntityId(event.target.value);
              setPage(0);
            }}
            placeholder="Paste a record UUID"
            className={filterControlClass}
          />
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
      ) : audit.isLoading ? (
        <LoadingState label="Loading audit trail" />
      ) : audit.error ? (
        <ErrorState message={audit.error} onRetry={audit.reload} />
      ) : rows.length === 0 ? (
        <EmptyState
          title="No audit events for this selection"
          description="Audit events are recorded as agents create lots, record weighments, run auctions, confirm winners, record payments and move returnable assets."
          icon="history"
        />
      ) : (
        <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
          <DataTable columns={columns} rows={rows} rowKey={(row) => row.id} caption="Audit trail" />
          <div className="px-3 py-2 border-t border-surface-container-high">
            <Pagination
              page={audit.data?.page ?? 0}
              size={size}
              totalElements={audit.data?.totalElements ?? 0}
              totalPages={audit.data?.totalPages ?? 0}
              onPageChange={setPage}
              onSizeChange={(next) => {
                setSize(next);
                setPage(0);
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
}
