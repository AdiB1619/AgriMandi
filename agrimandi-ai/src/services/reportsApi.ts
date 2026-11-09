import { api } from './api';

/**
 * Typed client for the operational reporting, oversight and audit endpoints.
 *
 * All filtering and pagination is executed by the backend: the UI only sends filter
 * parameters and renders the response. It never filters a full dataset in the browser.
 */

export type ReportPeriod = 'TODAY' | 'YESTERDAY' | 'CURRENT_WEEK' | 'CURRENT_MONTH' | 'CUSTOM';

export const PERIOD_OPTIONS: { value: ReportPeriod; label: string }[] = [
  { value: 'TODAY', label: 'Today' },
  { value: 'YESTERDAY', label: 'Yesterday' },
  { value: 'CURRENT_WEEK', label: 'Current week' },
  { value: 'CURRENT_MONTH', label: 'Current month' },
  { value: 'CUSTOM', label: 'Custom range' },
];

export interface DateRange {
  from: string;
  to: string;
}

export interface TransactionAggregate {
  totalTransactions: number | null;
  grossSaleValue: number | null;
  netSaleValue: number | null;
  totalQuantity: number | null;
  averageSaleRate: number | null;
}

export interface PaymentAggregate {
  totalTransactions: number | null;
  totalBilled: number | null;
  totalReceived: number | null;
  totalOutstanding: number | null;
  partiallyPaidCount: number | null;
  settledCount: number | null;
}

export interface AssetAggregate {
  assetRecords: number | null;
  totalIssued: number | null;
  totalReturned: number | null;
  totalOutstanding: number | null;
}

export interface AuctionAggregate {
  totalLots: number | null;
  totalAuctions: number | null;
  completedAuctions: number | null;
  activeAuctions: number | null;
  tieCases: number | null;
  transactionsGenerated: number | null;
  totalQuantityAuctioned: number | null;
}

export interface StatusCount {
  status: string;
  count: number;
}

export interface BuyerActivity {
  buyerId: string;
  buyerName: string;
  businessName: string | null;
  purchases: number | null;
  totalPurchaseValue: number | null;
  totalQuantity: number | null;
  outstandingPayment: number | null;
  outstandingAssetRecords: number | null;
}

export interface FarmerActivity {
  farmerId: string;
  farmerName: string;
  lots: number | null;
  totalQuantity: number | null;
  transactions: number | null;
  totalTransactionValue: number | null;
  outstandingPayment: number | null;
}

export interface TransactionSummaryResponse {
  range: DateRange;
  marketId: string;
  aggregate: TransactionAggregate;
  statusBreakdown: StatusCount[];
  totalOutstandingPayment: number | null;
}

export interface PaymentSummaryResponse {
  range: DateRange;
  marketId: string;
  aggregate: PaymentAggregate;
  statusBreakdown: StatusCount[];
}

export interface AssetSummaryResponse {
  range: DateRange;
  marketId: string;
  aggregate: AssetAggregate;
}

export interface AuctionSummaryResponse {
  range: DateRange;
  marketId: string;
  aggregate: AuctionAggregate;
}

export interface BuyerSummaryResponse {
  range: DateRange;
  marketId: string;
  buyers: BuyerActivity[];
}

export interface FarmerSummaryResponse {
  range: DateRange;
  marketId: string;
  farmers: FarmerActivity[];
}

export interface PaymentLedgerRow {
  transactionId: string;
  transactionNumber: string;
  transactionDate: string;
  lotId: string;
  lotNumber: string;
  commodity: string;
  farmerId: string;
  farmerName: string;
  buyerId: string;
  buyerName: string;
  totalAmount: number | null;
  paidAmount: number | null;
  outstandingAmount: number | null;
  paymentStatus: string;
  latestPaymentDate: string | null;
}

export interface AssetLedgerRow {
  transactionId: string;
  transactionNumber: string;
  lotNumber: string;
  assetTypeId: string;
  assetTypeName: string;
  buyerId: string;
  buyerName: string;
  quantityIssued: number | null;
  quantityReturned: number | null;
  quantityOutstanding: number | null;
  issueDate: string | null;
  latestReturnDate: string | null;
  assetStatus: string;
}

export interface AuditActivity {
  id: string;
  action: string;
  entityType: string;
  entityId: string;
  actorUsername: string | null;
  actorRole: string | null;
  timestamp: string;
}

export interface AuditLogRow {
  id: string;
  action: string;
  entityType: string;
  entityId: string;
  actorUsername: string | null;
  actorRole: string | null;
  actorId: string | null;
  marketId: string | null;
  marketCode: string | null;
  oldValue: string | null;
  newValue: string | null;
  timestamp: string;
}

export interface ExceptionCount {
  label: string;
  count: number | null;
}

export interface AdminOversightResponse {
  marketId: string;
  generatedAt: string;
  todayLots: { total: number | null };
  todayAuctionValue: { total: number | null };
  todayTransactions: { total: number | null };
  todayPaymentCollection: { total: number | null };
  totalOutstandingPayments: { total: number | null };
  outstandingAssets: AssetAggregate;
  activeAuctions: { total: number | null };
  recentTransactions: PaymentLedgerRow[];
  recentAuditActivity: AuditActivity[];
  topBuyersByValue: BuyerActivity[];
  farmerActivity: FarmerActivity[];
  exceptions: ExceptionCount[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface PeriodFilter {
  period: ReportPeriod;
  from?: string;
  to?: string;
}

export interface PaymentLedgerFilter extends PeriodFilter {
  status?: string;
  buyerId?: string;
  farmerId?: string;
  commodityId?: string;
  search?: string;
  page: number;
  size: number;
}

export interface AssetLedgerFilter extends PeriodFilter {
  buyerId?: string;
  search?: string;
  page: number;
  size: number;
}

export interface AuditFilter extends PeriodFilter {
  actorId?: string;
  action?: string;
  entityType?: string;
  entityId?: string;
  page: number;
  size: number;
}

function periodParams(filter: PeriodFilter): URLSearchParams {
  const params = new URLSearchParams();
  params.set('period', filter.period);
  if (filter.period === 'CUSTOM') {
    if (filter.from) params.set('from', filter.from);
    if (filter.to) params.set('to', filter.to);
  }
  return params;
}

function appendIfPresent(params: URLSearchParams, key: string, value?: string): void {
  if (value !== undefined && value !== null && value !== '') {
    params.set(key, value);
  }
}

function queryString(params: URLSearchParams): string {
  const asString = params.toString();
  return asString ? `?${asString}` : '';
}

function buyerParams(filter: PeriodFilter & { buyerId?: string }): URLSearchParams {
  const params = periodParams(filter);
  appendIfPresent(params, 'buyerId', filter.buyerId);
  return params;
}

export const reportsApi = {
  transactionSummary: (filter: PeriodFilter) =>
    api.get<TransactionSummaryResponse>(`/api/reports/transactions${queryString(periodParams(filter))}`),

  paymentSummary: (filter: PeriodFilter) =>
    api.get<PaymentSummaryResponse>(`/api/reports/payments${queryString(periodParams(filter))}`),

  assetSummary: (filter: PeriodFilter) =>
    api.get<AssetSummaryResponse>(`/api/reports/assets${queryString(periodParams(filter))}`),

  auctionSummary: (filter: PeriodFilter) =>
    api.get<AuctionSummaryResponse>(`/api/reports/auctions${queryString(periodParams(filter))}`),

  buyerSummary: (filter: PeriodFilter & { buyerId?: string }) =>
    api.get<BuyerSummaryResponse>(`/api/reports/buyers${queryString(buyerParams(filter))}`),

  farmerSummary: (filter: PeriodFilter & { farmerId?: string }) => {
    const params = periodParams(filter);
    appendIfPresent(params, 'farmerId', filter.farmerId);
    return api.get<FarmerSummaryResponse>(`/api/reports/farmers${queryString(params)}`);
  },

  paymentLedger: (filter: PaymentLedgerFilter) => {
    const params = periodParams(filter);
    appendIfPresent(params, 'status', filter.status);
    appendIfPresent(params, 'buyerId', filter.buyerId);
    appendIfPresent(params, 'farmerId', filter.farmerId);
    appendIfPresent(params, 'commodityId', filter.commodityId);
    appendIfPresent(params, 'search', filter.search);
    params.set('page', String(filter.page));
    params.set('size', String(filter.size));
    return api.get<PageResponse<PaymentLedgerRow>>(`/api/reports/payment-ledger${queryString(params)}`);
  },

  assetLedger: (filter: AssetLedgerFilter) => {
    const params = periodParams(filter);
    appendIfPresent(params, 'buyerId', filter.buyerId);
    appendIfPresent(params, 'search', filter.search);
    params.set('page', String(filter.page));
    params.set('size', String(filter.size));
    return api.get<PageResponse<AssetLedgerRow>>(`/api/reports/asset-ledger${queryString(params)}`);
  },

  adminOversight: () => api.get<AdminOversightResponse>('/api/reports/overview'),
};

export const auditApi = {
  search: (filter: AuditFilter) => {
    const params = periodParams(filter);
    appendIfPresent(params, 'actorId', filter.actorId);
    appendIfPresent(params, 'action', filter.action);
    appendIfPresent(params, 'entityType', filter.entityType);
    appendIfPresent(params, 'entityId', filter.entityId);
    params.set('page', String(filter.page));
    params.set('size', String(filter.size));
    return api.get<PageResponse<AuditLogRow>>(`/api/audit-logs${queryString(params)}`);
  },

  actions: () => api.get<string[]>('/api/audit-logs/actions'),
};

// ── Reference data for the filter controls ───────────────────────────────────

export interface FarmerRef {
  id: string;
  farmerCode: string | null;
  name: string;
}

export interface BuyerRef {
  id: string;
  buyerCode: string | null;
  name: string;
  businessName: string | null;
}

export interface CommodityRef {
  id: string;
  name: string;
}

export const referenceApi = {
  farmers: () => api.get<FarmerRef[]>('/api/farmers'),
  buyers: () => api.get<BuyerRef[]>('/api/buyers'),
  commodities: () => api.get<CommodityRef[]>('/api/commodities'),
};
