package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.DniData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@ConditionalOnProperty(name = "app.dni.mode", havingValue = "mock")
public class DniProcessingService {

    //private final VisionService visionService;
    private final DocumentAiService documentAiService;
    private final DniParserService dniParserService;
    private final AdvancedImageProcessingService imageProcessingService;
    private final ImagePreprocessingService imagePreprocessingService;

    public DniProcessingService(/*VisionService visionService,*/ DocumentAiService documentAiService,
                                                                 DniParserService dniParserService,
                                                                 AdvancedImageProcessingService imageProcessingService,
                                                                 ImagePreprocessingService imagePreprocessingService) {
        //this.visionService = visionService;
        this.documentAiService = documentAiService;
        this.dniParserService = dniParserService;
        this.imageProcessingService = imageProcessingService;
        this.imagePreprocessingService = imagePreprocessingService;
    }

    public DniData processDni(MultipartFile frontImage, MultipartFile backImage) throws Exception {
        System.out.println("\n========================================");
        System.out.println("PROCESANDO DNI (SIN BASE DE DATOS)");
        System.out.println("========================================\n");

        try {
            long startTime = System.currentTimeMillis();

            // 0. PREPROCESAR IMÁGENES EN PARALELO
            System.out.println("🔄 Preprocesando imágenes en paralelo...");
            CompletableFuture<MultipartFile> frontPreprocessFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return imagePreprocessingService.preprocessImage(frontImage);
                } catch (Exception e) {
                    throw new RuntimeException("Error preprocesando imagen frontal", e);
                }
            });

            CompletableFuture<MultipartFile> backPreprocessFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return imagePreprocessingService.preprocessImage(backImage);
                } catch (Exception e) {
                    throw new RuntimeException("Error preprocesando imagen trasera", e);
                }
            });

            MultipartFile processedFrontImage = frontPreprocessFuture.get();
            MultipartFile processedBackImage = backPreprocessFuture.get();
            long preprocessTime = System.currentTimeMillis() - startTime;
            System.out.println("✅ Imágenes preprocesadas en " + preprocessTime + "ms");

            // 1. EXTRAER TEXTO CON GOOGLE DOCUMENT AI EN PARALELO
            System.out.println("🔍 Extrayendo texto en paralelo...");
            long ocrStartTime = System.currentTimeMillis();

            CompletableFuture<String> frontTextFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return documentAiService.extractTextFromImage(processedFrontImage);
                } catch (Exception e) {
                    throw new RuntimeException("Error extrayendo texto frontal", e);
                }
            });

            CompletableFuture<String> backTextFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return documentAiService.extractTextFromImage(processedBackImage);
                } catch (Exception e) {
                    throw new RuntimeException("Error extrayendo texto trasero", e);
                }
            });

            String frontText = frontTextFuture.get();
            String backText = backTextFuture.get();
            long ocrTime = System.currentTimeMillis() - ocrStartTime;
            System.out.println("✅ Texto extraído exitosamente en " + ocrTime + "ms");

            // 2. PARSEAR DATOS DEL DNI
            System.out.println("📊 Parseando datos del DNI...");
            long parseStartTime = System.currentTimeMillis();
            DniData extractedData = dniParserService.parseDniData(frontText, backText);

            if (extractedData == null) {
                throw new RuntimeException("No se pudieron extraer los datos del DNI.");
            }

            String dniFinal = extractedData.numeroDni();

            if (extractedData.numeroDni() != null) {
                System.out.println("🔍 Validando número de DNI...");

                // Si detectamos que el OCR leyó algo distinto al MRZ,
                // el MRZ siempre tiene la prioridad por ser un estándar internacional.
                // Ejemplo: OCR leyó 72938997 pero MRZ dice 72838997
                // Aquí podrías agregar una lógica de log para auditoría:
                // System.out.println("⚠️ Discrepancia detectada. Prevalece MRZ.");
            }
            long parseTime = System.currentTimeMillis() - parseStartTime;
            System.out.println("✅ Datos parseados en " + parseTime + "ms");

            // 3. EXTRAER FOTO Y CONVERTIR IMÁGENES EN PARALELO
            System.out.println("📸 Procesando imágenes en paralelo...");
            long imageProcessStartTime = System.currentTimeMillis();

            CompletableFuture<String> photoExtractionFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    BufferedImage preprocessedBI = ImageIO.read(processedFrontImage.getInputStream());
                    byte[] fotoPersona = imageProcessingService.extractPersonPhoto(preprocessedBI);
                    return Base64.getEncoder().encodeToString(fotoPersona);
                } catch (Exception e) {
                    throw new RuntimeException("Error extrayendo foto", e);
                }
            });

            CompletableFuture<String> frontBase64Future = CompletableFuture.supplyAsync(() -> {
                try {
                    return Base64.getEncoder().encodeToString(frontImage.getBytes());
                } catch (Exception e) {
                    throw new RuntimeException("Error convirtiendo imagen frontal", e);
                }
            });

            CompletableFuture<String> backBase64Future = CompletableFuture.supplyAsync(() -> {
                try {
                    return Base64.getEncoder().encodeToString(backImage.getBytes());
                } catch (Exception e) {
                    throw new RuntimeException("Error convirtiendo imagen trasera", e);
                }
            });

            String fotoPersonaBase64 = photoExtractionFuture.get();
            String frontImageBase64 = frontBase64Future.get();
            String backImageBase64 = backBase64Future.get();
            long imageProcessTime = System.currentTimeMillis() - imageProcessStartTime;
            System.out.println("✅ Imágenes procesadas en " + imageProcessTime + "ms");

            System.out.println("========================================");
            System.out.println("PROCESAMIENTO COMPLETADO");
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("⏱️ Tiempo total: " + totalTime + "ms");
            System.out.println("  - Preprocesamiento: " + preprocessTime + "ms");
            System.out.println("  - OCR: " + ocrTime + "ms");
            System.out.println("  - Parseo: " + parseTime + "ms");
            System.out.println("  - Procesamiento imágenes: " + imageProcessTime + "ms");
            System.out.println("========================================\n");

            // 5. DEVOLVER DATOS COMPLETOS SIN PERSISTIR
            return new DniData(
                    extractedData.numeroDni(),
                    extractedData.apellidos(),
                    extractedData.nombres(),
                    extractedData.fechaNacimiento(),
                    extractedData.sexo(),
                    extractedData.nacionalidad(),
                    extractedData.fechaEmision(),
                    extractedData.fechaVencimiento(),
                    fotoPersonaBase64,
                    frontImageBase64,
                    backImageBase64
            );

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error en procesamiento paralelo: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error en procesamiento paralelo del DNI", e);
        } catch (Exception e) {
            System.err.println("❌ Error procesando DNI: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}