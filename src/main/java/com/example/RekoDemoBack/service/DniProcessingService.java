package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.DniData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Base64;

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
            // 0. PREPROCESAR IMÁGENES
            System.out.println("🔄 Preprocesando imágenes...");
            MultipartFile processedFrontImage = imagePreprocessingService.preprocessImage(frontImage);
            MultipartFile processedBackImage = imagePreprocessingService.preprocessImage(backImage);
            System.out.println("✅ Imágenes preprocesadas");

            // 1. EXTRAER TEXTO CON GOOGLE VISION
            System.out.println("🔍 Extrayendo texto...");
            /*String frontText = visionService.extractTextFromImageEnhanced(processedFrontImage);
            String backText = visionService.extractTextFromImageEnhanced(processedBackImage);*/
            String frontText = documentAiService.extractTextFromImage(processedFrontImage);
            String backText = documentAiService.extractTextFromImage(processedBackImage);
            System.out.println("✅ Texto extraído exitosamente");

            // 2. PARSEAR DATOS DEL DNI
            System.out.println("📊 Parseando datos del DNI...");
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

            // 3. EXTRAER FOTO DE LA PERSONA
            System.out.println("📸 Extrayendo foto del DNI...");
            BufferedImage preprocessedBI = ImageIO.read(processedFrontImage.getInputStream());

            // Llamamos al método correcto del servicio ImageProcessingService
            byte[] fotoPersona = imageProcessingService.extractPersonPhoto(preprocessedBI);
            String fotoPersonaBase64 = Base64.getEncoder().encodeToString(fotoPersona);
            System.out.println("✅ Foto extraída exitosamente");

            // 4. CONVERTIR IMÁGENES ORIGINALES A BASE64
            System.out.println("🖼️ Convirtiendo imágenes a Base64...");
            String frontImageBase64 = Base64.getEncoder().encodeToString(frontImage.getBytes());
            String backImageBase64 = Base64.getEncoder().encodeToString(backImage.getBytes());
            System.out.println("✅ Imágenes convertidas");

            System.out.println("========================================");
            System.out.println("PROCESAMIENTO COMPLETADO");
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

        } catch (Exception e) {
            System.err.println("❌ Error procesando DNI: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}