package com.example.RekoDemoBack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

@Service
public class RekognitionComparisonService {

    private final RekognitionClient rekClient;

    @Value("${api.reniec.base-url}")
    private String baseUrl;


    public RekognitionComparisonService(
            @Value("${aws.access-key-id}") String awsAccessKeyId,
            @Value("${aws.secret-access-key}") String awsSecretAccessKey,
            @Value("${aws.region}") String awsRegion
    ) {

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                awsAccessKeyId,
                awsSecretAccessKey
        );

        this.rekClient = RekognitionClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    /**
     * Compara dos imágenes y retorna el resultado
     */
    public CompareFacesResponse compareFaces(byte[] sourceImageBytes, byte[] targetImageBytes) {
        Image sourceImage = Image.builder()
                .bytes(SdkBytes.fromByteArray(sourceImageBytes))
                .build();

        Image targetImage = Image.builder()
                .bytes(SdkBytes.fromByteArray(targetImageBytes))
                .build();

        CompareFacesRequest request = CompareFacesRequest.builder()
                .sourceImage(sourceImage)
                .targetImage(targetImage)
                .similarityThreshold(70.0F) // Umbral de similitud
                .qualityFilter(QualityFilter.AUTO) // Filtro de calidad automático
                .build();

        return rekClient.compareFaces(request);
    }

    /**
     * Compara dos imágenes en formato Base64
     */
    public CompareFacesResponse compareFacesBase64(String sourceBase64, String targetBase64) {
        byte[] sourceBytes = decodeBase64(sourceBase64);
        byte[] targetBytes = decodeBase64(targetBase64);

        return compareFaces(sourceBytes, targetBytes);
    }

    /**
     * Compara una imagen con la imagen de referencia de una sesión de liveness
     */
    public CompareFacesResponse compareWithLivenessReference(String sessionId, byte[] targetImageBytes) {
        // Primero necesitamos obtener la imagen de referencia del liveness
        // Inyectaremos RekognitionLivenessService para esto
        // (esto se manejará en el controlador)

        // Este método es solo para la estructura
        // La lógica completa estará en el controlador
        return null;
    }

    /**
     * Detección de rostros en una imagen
     */
    public DetectFacesResponse detectFaces(byte[] imageBytes) {
        Image image = Image.builder()
                .bytes(SdkBytes.fromByteArray(imageBytes))
                .build();

        DetectFacesRequest request = DetectFacesRequest.builder()
                .image(image)
                .attributes(Attribute.ALL) // Obtener todos los atributos
                .build();

        return rekClient.detectFaces(request);
    }

    /**
     * Convierte Base64 a bytes
     */
    public byte[] decodeBase64(String base64String) {
        String cleanBase64;
        if (base64String.contains(",")) {
            cleanBase64 = base64String.substring(base64String.indexOf(",") + 1);
        } else {
            cleanBase64 = base64String;
        }
        return java.util.Base64.getDecoder().decode(cleanBase64);
    }
}