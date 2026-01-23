package com.example.RekoDemoBack.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.ByteString;

@Service
public class VisionService {

    /**
     * Llama a la API de Cloud Vision y extrae el texto completo de una imagen.
     */
    public String extractTextFromImage(MultipartFile imageFile) throws IOException {

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("La imagen es requerida y no puede estar vacía.");
        }

        // Validar tamaño máximo (10MB)
        if (imageFile.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen es demasiado grande. Máximo 10MB.");
        }

        // Validar tipo de contenido
        String contentType = imageFile.getContentType();
        if (contentType == null || !isValidImageType(contentType)) {
            throw new IllegalArgumentException("Formato de imagen no válido. Usa JPEG o PNG.");
        }

        // Usamos try-with-resources para cerrar el cliente automáticamente
        try (ImageAnnotatorClient visionClient = ImageAnnotatorClient.create()) {

            byte[] bytes = imageFile.getBytes();
            ByteString byteString = ByteString.copyFrom(bytes);
            Image image = Image.newBuilder().setContent(byteString).build();

            // Usamos DOCUMENT_TEXT_DETECTION por ser más preciso para OCR
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(
                    Collections.singletonList(request)
            );

            AnnotateImageResponse firstResponse = response.getResponses(0);

            if (firstResponse.hasError()) {
                throw new RuntimeException("Error de Google Vision: " + firstResponse.getError().getMessage());
            }

            // Devolvemos el texto completo detectado
            TextAnnotation annotation = firstResponse.getFullTextAnnotation();
            String extractedText = annotation.getText();
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new RuntimeException("No se pudo extraer texto de la imagen. Verifica que sea un documento válido.");
            }

            return extractedText;

        } catch (IOException e) {
            throw new IOException("Error al procesar la imagen: " + e.getMessage(), e);
        }
    }

    /**
     * Valida que el tipo MIME sea una imagen válida
     */
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") || 
                contentType.equals("image/png") || 
                contentType.equals("image/jpg")
        );
    }
    public String extractTextFromImageEnhanced(MultipartFile imageFile) throws IOException {
        try (ImageAnnotatorClient visionClient = ImageAnnotatorClient.create()) {

            byte[] bytes = imageFile.getBytes();
            ByteString byteString = ByteString.copyFrom(bytes);
            Image image = Image.newBuilder().setContent(byteString).build();

            // Configurar características mejoradas para documentos
            Feature textFeature = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build();

            Feature denseTextFeature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

            // Crear requests para ambos tipos de detección
            AnnotateImageRequest documentRequest = AnnotateImageRequest.newBuilder()
                    .addFeatures(textFeature)
                    .setImage(image)
                    .build();

            AnnotateImageRequest denseTextRequest = AnnotateImageRequest.newBuilder()
                    .addFeatures(denseTextFeature)
                    .setImage(image)
                    .build();

            // Procesar con ambas estrategias
            List<AnnotateImageRequest> requests = List.of(documentRequest, denseTextRequest);
            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(requests);

            // Combinar resultados
            StringBuilder combinedText = new StringBuilder();

            for (AnnotateImageResponse singleResponse : response.getResponsesList()) {
                if (singleResponse.hasError()) {
                    System.err.println("Error en OCR: " + singleResponse.getError().getMessage());
                    continue;
                }

                TextAnnotation annotation = singleResponse.getFullTextAnnotation();
                if (annotation != null && annotation.getText() != null) {
                    String text = annotation.getText().trim();
                    if (!text.isEmpty()) {
                        if (combinedText.length() > 0) combinedText.append("\n---\n");
                        combinedText.append(text);
                    }
                }
            }

            String finalText = combinedText.toString();
            if (finalText.isEmpty()) {
                throw new RuntimeException("No se pudo extraer texto de la imagen");
            }

            System.out.println("📝 Texto extraído (longitud: " + finalText.length() + " caracteres)");
            return finalText;

        } catch (IOException e) {
            throw new IOException("Error al procesar la imagen: " + e.getMessage(), e);
        }
    }
}
