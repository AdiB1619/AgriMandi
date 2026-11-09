package com.agrimandi.api.repository;

import com.agrimandi.api.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findByAuctionIdOrderByBidAmountDesc(UUID auctionId);
    
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId AND b.status = 'ACTIVE' ORDER BY b.bidAmount DESC")
    List<Bid> findActiveBidsByAuctionIdOrderByAmountDesc(@Param("auctionId") UUID auctionId);
}
