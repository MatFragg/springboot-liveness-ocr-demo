package com.example.RekoDemoBack.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ACJ_Auth {
    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("tokenType")
    private String tokenType;

    @JsonProperty("expireln")  // IMPORTANTE: nombre exacto del JSON
    private Long expireln;
}