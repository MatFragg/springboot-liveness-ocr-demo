package com.example.RekoDemoBack.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FacialValidationRequest {
    @JsonProperty("serialNumber")
    private String serialNumber;

    @JsonProperty("template")
    private String template; // Imagen facial en base64

    @JsonProperty("type")
    private String type = "R"; // Valor por defecto según documentación

    @JsonProperty("quality")
    private String quality = "7"; // Valor por defecto según ejemplo

    @JsonProperty("documentNumber")
    private String documentNumber;
}