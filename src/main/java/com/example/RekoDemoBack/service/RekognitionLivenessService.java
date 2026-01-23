package com.example.RekoDemoBack.service;

// 1. IMPORTA ESTAS CLASES
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class RekognitionLivenessService {

    private final RekognitionClient rekClient;

    public RekognitionLivenessService(
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

    public String createSession() {
        CreateFaceLivenessSessionRequest req = CreateFaceLivenessSessionRequest.builder().build();
        CreateFaceLivenessSessionResponse resp = rekClient.createFaceLivenessSession(req);
        return resp.sessionId();
    }

    public GetFaceLivenessSessionResultsResponse getSessionResults(String sessionId) {
        GetFaceLivenessSessionResultsRequest req = GetFaceLivenessSessionResultsRequest.builder()
                .sessionId(sessionId)
                .build();
        return rekClient.getFaceLivenessSessionResults(req);
    }

    /**
     * Procesa una imagen para que tenga dimensiones y encuadre tipo carnet (3:4)
     */
    public byte[] processToPassportSize(byte[] imageBytes) {
        try {
            // 1. Detectar el rostro para obtener las coordenadas
            SdkBytes sdkBytes = SdkBytes.fromByteArray(imageBytes);
            DetectFacesRequest detectRequest = DetectFacesRequest.builder()
                    .image(Image.builder().bytes(sdkBytes).build())
                    .build();
            DetectFacesResponse detectResponse = rekClient.detectFaces(detectRequest);

            if (detectResponse.faceDetails().isEmpty()) return imageBytes;

            // Tomamos el primer rostro detectado
            FaceDetail face = detectResponse.faceDetails().get(0);
            BoundingBox box = face.boundingBox();

            // 2. Cargar la imagen original en memoria
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            int width = original.getWidth();
            int height = original.getHeight();

            // 3. Calcular el área de recorte (Passport Padding)
            // Queremos que el rostro ocupe un 50-60% de la imagen, con espacio arriba
            int faceWidth = Math.round(box.width() * width);
            int faceHeight = Math.round(box.height() * height);

            // Definimos el ancho del recorte basado en el rostro + margen lateral
            int cropWidth = (int) (faceWidth * 2.0);
            int cropHeight = (int) (cropWidth * 1.33); // Proporción 3:4

            // Calculamos el centro del rostro para posicionar el recorte
            int centerX = Math.round((box.left() + box.width() / 2) * width);
            int centerY = Math.round((box.top() + box.height() / 2) * height);

            // Coordenadas del punto superior izquierdo del recorte
            int left = Math.max(0, centerX - (cropWidth / 2));
            int top = Math.max(0, centerY - (int)(cropHeight * 0.40)); // Margen superior para la cabeza

            // Ajustar si el recorte excede los límites de la imagen original
            if (left + cropWidth > width) left = width - cropWidth;
            if (top + cropHeight > height) top = height - cropHeight;

            // Verificación final de seguridad para evitar subimage negativa
            left = Math.max(0, left);
            top = Math.max(0, top);
            cropWidth = Math.min(cropWidth, width - left);
            cropHeight = Math.min(cropHeight, height - top);

            // 4. Realizar el recorte
            BufferedImage cropped = original.getSubimage(left, top, cropWidth, cropHeight);

            // 5. Convertir a bytes de nuevo
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(cropped, "jpg", baos);
            return baos.toByteArray();

        } catch (IOException | RuntimeException e) {
            return imageBytes; // Si algo falla, devolvemos la original para no romper el flujo
        }
    }
}