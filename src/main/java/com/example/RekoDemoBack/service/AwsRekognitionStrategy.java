package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.FaceComparisonResult;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.rekognition.model.CompareFacesResponse;
import software.amazon.awssdk.services.rekognition.model.CompareFacesMatch;

@Service
public class AwsRekognitionStrategy implements FaceComparisonStrategy {

    private final RekognitionComparisonService rekService; // Tu servicio existente

    public AwsRekognitionStrategy(RekognitionComparisonService rekService) {
        this.rekService = rekService;
    }

    @Override
    public FaceComparisonResult compareFaces(String imageFirst, String imageSecond) {
        // Reutilizamos tu lógica existente que acepta Base64
        CompareFacesResponse awsResponse = rekService.compareFacesBase64(imageFirst, imageSecond);

        FaceComparisonResult result = new FaceComparisonResult();
        result.setProvider(getProviderName());

        if (!awsResponse.faceMatches().isEmpty()) {
            CompareFacesMatch match = awsResponse.faceMatches().get(0);
            result.setSimilarityScore(match.similarity().doubleValue());
            result.setMatch(match.similarity() > 70.0); // Tu umbral AWS
            result.setResultCode("000"); // Simulamos código OK
            result.setResultMessage("Coincidencia encontrada AWS");
        } else {
            result.setSimilarityScore(0.0);
            result.setMatch(false);
            result.setResultCode("NO_MATCH");
        }

        return result;
    }

    @Override
    public String getProviderName() {
        return "aws";
    }
}