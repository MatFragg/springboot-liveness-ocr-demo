package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.FaceComparisonResult;

/**
 * Interfaz Strategy para comparación facial
 * Permite múltiples implementaciones (AWS, RENIEC, etc.)
 */
public interface FaceComparisonStrategy {

    /**
     * Compara dos imágenes faciales
     * @param imageFirst Primera imagen en Base64
     * @param imageSecond Segunda imagen en Base64
     * @return Resultado unificado de la comparación
     */
    FaceComparisonResult compareFaces(String imageFirst, String imageSecond);

    /**
     * Obtiene el nombre del proveedor
     * @return Nombre del proveedor (aws, reniec, etc.)
     */
    String getProviderName();
}