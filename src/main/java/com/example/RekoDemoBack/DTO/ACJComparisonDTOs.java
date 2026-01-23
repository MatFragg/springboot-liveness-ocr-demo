package com.example.RekoDemoBack.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

public class ACJComparisonDTOs {

    // Request Body
    @Data
    @AllArgsConstructor
    public static class ACJComparisonRequest {
        private String imageFirst;
        private String imageSecond;
    }

    // Response Body
    @Data
    @NoArgsConstructor
    public static class ACJComparisonResponse {
        private Result result;
        private Data data;

        @lombok.Data
        public static class Result {
            private String code;
            private String info;
        }

        @lombok.Data
        public static class Data {
            private Double similarityScore;
            private boolean match;
        }
    }

    // Auth Response
    @Data
    public static class ACJ_Auth {
        private String accessToken;
        private String tokenType;
        private int expireIn;
    }
}