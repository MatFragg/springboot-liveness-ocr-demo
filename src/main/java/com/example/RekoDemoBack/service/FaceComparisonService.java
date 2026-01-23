package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.FaceComparisonResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FaceComparisonService {

    private final Map<String, FaceComparisonStrategy> strategies;

    // Inyectamos el valor definido en application.properties
    // Si no existe, por defecto usará "aws"
    @Value("${face.comparison.provider:aws}")
    private String defaultProvider;

    // Spring es inteligente: si pides una List<Interfaz>, te da todas las implementaciones
    public FaceComparisonService(List<FaceComparisonStrategy> strategyList) {
        // Convertimos la lista a un Mapa: "aws" -> AwsStrategy, "reniec" -> ACJStrategy
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }

    /**
     * Método principal: Usa el proveedor definido en el archivo properties
     */
    public FaceComparisonResult compareFaces(String imageFirst, String imageSecond) {
        return compareFaces(imageFirst, imageSecond, this.defaultProvider);
    }

    /**
     * Sobrecarga: Permite forzar un proveedor específico si fuera necesario
     */
    public FaceComparisonResult compareFaces(String imageFirst, String imageSecond, String provider) {
        System.out.println("🔍 Usando proveedor: " + provider);

        FaceComparisonStrategy strategy = strategies.get(provider.toLowerCase());

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "El proveedor '" + provider + "' no está implementado o mal configurado."
            );
        }

        return strategy.compareFaces(imageFirst, imageSecond);
    }
}