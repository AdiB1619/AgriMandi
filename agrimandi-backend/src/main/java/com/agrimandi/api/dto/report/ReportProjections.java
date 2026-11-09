package com.agrimandi.api.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Closed projections for the reporting aggregations.
 *
 * <p>Aggregate queries are declared with aliases and mapped onto these read-only views, so the
 * database does the arithmetic and the service layer only converts the projection into the
 * response DTO.
 */
public class ReportProjections {

    private ReportProjections() {
    }

    public interface TransactionAggregateView {
        Long getTotalTransactions();

        BigDecimal getGrossSaleValue();

        BigDecimal getNetSaleValue();

        BigDecimal getTotalQuantity();

        BigDecimal getAverageSaleRate();

        /**
         * Number of distinct price units in the period. A single average rate is only meaningful
         * when every transaction was priced in the same unit.
         */
        Long getDistinctPriceUnits();
    }

    public interface CountView {
        Long getTotal();
    }

    public interface AssetAggregateView {
        Long getAssetRecords();

        Long getTotalIssued();

        Long getTotalReturned();

        Long getTotalOutstanding();
    }

    public interface AuctionAggregateView {
        Long getTotalAuctions();

        Long getCompletedAuctions();

        Long getActiveAuctions();

        Long getTieCases();

        BigDecimal getTotalQuantityAuctioned();
    }

    public interface StatusCountView {
        String getStatus();

        Long getCount();
    }

    public interface BuyerActivityView {
        UUID getBuyerId();

        String getBuyerName();

        String getBusinessName();

        Long getPurchases();

        BigDecimal getTotalPurchaseValue();

        BigDecimal getTotalQuantity();
    }

    public interface FarmerActivityView {
        UUID getFarmerId();

        String getFarmerName();

        BigDecimal getTotalQuantity();

        Long getTransactions();

        BigDecimal getTotalTransactionValue();
    }

    public interface IdCountView {
        UUID getOwnerId();

        Long getTotal();
    }

    public interface OwnerAmountView {
        UUID getOwnerId();

        BigDecimal getTotal();
    }
}
