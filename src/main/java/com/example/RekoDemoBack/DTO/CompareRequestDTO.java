package com.example.RekoDemoBack.DTO;
import lombok.Data;

@Data
public class CompareRequestDTO {
    private String sourceImageBase64; // Imagen 1 en Base64
    private String targetImageBase64; // Imagen 2 en Base64
    private String livenessSessionId; // Opcional: ID de sesión de liveness
    private Float similarityThreshold; // Opcional: Umbral personalizado
    private Boolean includeFaceDetails; // Opcional: Incluir detalles de rostros
}
