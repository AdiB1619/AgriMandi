package com.agrimandi.api.dto;

import lombok.Data;
import java.util.UUID;

public class ReferenceDTOs {

    @Data
    public static class FarmerDTO {
        private UUID id;
        private String farmerCode;
        private String name;
        private String village;
        private String phone;
        private String address;
        private String status;
    }

    @Data
    public static class BuyerDTO {
        private UUID id;
        private String buyerCode;
        private String name;
        private String businessName;
        private String phone;
        private String address;
        private String licenseReference;
        private String status;
    }

    @Data
    public static class CommodityDTO {
        private UUID id;
        private String name;
        private String code;
        private String defaultPriceUnit;
        private Boolean activeStatus;
    }

    @Data
    public static class AssetTypeDTO {
        private UUID id;
        private String name;
        private String code;
        private Boolean returnable;
    }
}
