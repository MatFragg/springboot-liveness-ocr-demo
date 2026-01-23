package com.example.RekoDemoBack.service;

import com.google.cloud.documentai.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentAiServiceImpl implements DocumentAiService{

    // Define estas variables en tu application.properties
    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${google.cloud.location}") // ej: "us"
    private String location;

    @Value("${google.cloud.processor-id}") // El ID que copiaste del Document OCR
    private String processorId;

    @Override
    public String extractTextFromImage(MultipartFile file) throws IOException {
        String name = String.format("projects/%s/locations/%s/processors/%s",
                projectId, location, processorId);

        try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create()) {
            ByteString content = ByteString.copyFrom(file.getBytes());

            RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent(content)
                    .setMimeType("image/jpeg")
                    .build();

            ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(name)
                    .setRawDocument(rawDocument)
                    .build();

            ProcessResponse result = client.processDocument(request);
            Document document = result.getDocument();

            // --- NUEVA LÓGICA DE EXTRACCIÓN ESTRUCTURADA ---
            StringBuilder structuredData = new StringBuilder();

            // Recorremos las entidades (campos que tú etiquetaste en la consola)
            for (Document.Entity entity : document.getEntitiesList()) {
                String fieldName = entity.getType();
                String fieldValue = entity.getMentionText();
                float confidence = entity.getConfidence();

                // Concatenamos de una forma que tu Parser pueda entender fácilmente
                // Ejemplo: "primer_apellido: ALIAGA | confianza: 0.98"
                structuredData.append(fieldName).append(": ").append(fieldValue)
                        .append(" #CONFID# ").append(confidence).append("\n");

                // Si la entidad tiene sub-entidades (campos anidados)
                for (Document.Entity subEntity : entity.getPropertiesList()) {
                    structuredData.append("  -> ").append(subEntity.getType())
                            .append(": ").append(subEntity.getMentionText()).append("\n");
                }
            }

            // También incluimos el texto completo por si el Custom Extractor falla en algún campo
            structuredData.append("--- RAW TEXT START ---\n");
            structuredData.append(document.getText());

            return structuredData.toString();
        }
    }
}