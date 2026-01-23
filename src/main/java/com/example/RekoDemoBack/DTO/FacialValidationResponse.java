// FacialValidationResponse.java
package com.example.RekoDemoBack.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FacialValidationResponse {
    private Result result;
    private FacialValidationData data;

    @Data
    public static class Result {
        private String code;
        private String info;
    }

    @Data
    public static class FacialValidationData {
        @JsonProperty("reniecErrorCode")
        private Integer reniecErrorCode;

        @JsonProperty("reniecErrorDescription")
        private String reniecErrorDescription;

        @JsonProperty("documentType")
        private Integer documentType;

        @JsonProperty("documentNumber")
        private String documentNumber;

        @JsonProperty("personName")
        private String personName;

        @JsonProperty("personLastName")
        private String personLastName;

        @JsonProperty("personMotherLastName")
        private String personMotherLastName;

        @JsonProperty("expirationDate")
        private String expirationDate;

        @JsonProperty("validity")
        private String validity;

        @JsonProperty("restriction")
        private String restriction;

        @JsonProperty("restrictionGroup")
        private String restrictionGroup;

        @JsonProperty("traking")
        private String traking;
    }
}