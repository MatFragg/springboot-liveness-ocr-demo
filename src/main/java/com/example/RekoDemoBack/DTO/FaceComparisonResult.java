package com.example.RekoDemoBack.DTO;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO unificado para resultados de comparación facial
 * Independiente del proveedor (AWS, RENIEC, etc.)
 */
public class FaceComparisonResult {

    private boolean match;
    private double similarityScore;
    private String provider;
    private String resultCode;
    private String resultMessage;
    private Map<String, Object> metadata;

    public FaceComparisonResult() {
        this.metadata = new HashMap<>();
    }

    // Getters y Setters

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return "FaceComparisonResult{" +
                "match=" + match +
                ", similarityScore=" + similarityScore +
                ", provider='" + provider + '\'' +
                ", resultCode='" + resultCode + '\'' +
                ", resultMessage='" + resultMessage + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}