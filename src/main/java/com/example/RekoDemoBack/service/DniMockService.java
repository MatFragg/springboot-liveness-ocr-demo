package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.DniData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

@Service
@Primary
@ConditionalOnProperty(name = "app.dni.mode", havingValue = "real")
public class DniMockService implements IDniService {

    private final VisionService visionService;
    private final DniParserService dniParserService;
    private final Map<String, DniData> mockPool = new HashMap<>();

    @Value("classpath:mocks/mi_foto_base64.txt")
    private Resource faceResource;

    public DniMockService(VisionService visionService, DniParserService dniParserService) {
        this.visionService = visionService;
        this.dniParserService = dniParserService;
    }

    @jakarta.annotation.PostConstruct
    private void init() throws ParseException, IOException {
        setupMockData();
    }

    private void setupMockData() throws ParseException, IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        assert faceResource != null;
        String miFotoBase64 = StreamUtils.copyToString(faceResource.getInputStream(), StandardCharsets.UTF_8);
        mockPool.put("72838997", new DniData(
                "72838997", "ALIAGA AGUIRRE", "ETHAN MATIAS", "2006-03-30", "M", "PERUANA",
                sdf.parse("2023-05-29"), sdf.parse("2023-06-31"), miFotoBase64, null, null
        ));

        mockPool.put("11111111", new DniData(
                "11111111", "ALIAGA AGUIRRE", "ETHAN MATIAS", "1980-01-01", "M", "PERUANA",
                null, null, "FOTO_DUMMY", null, null
        ));
    }

    @Override
    public DniData processDni(MultipartFile frontImage, MultipartFile backImage) throws Exception {
        System.out.println("🧪 MODO HÍBRIDO: OCR REAL + DATA MOCK");

        // 1. EXTRAER TEXTO REAL USANDO GOOGLE VISION (Gratis/Low cost)
        String frontText = visionService.extractTextFromImageEnhanced(frontImage);
        String backText = visionService.extractTextFromImageEnhanced(backImage);

        // 2. PARSEAR EL NÚMERO DE DNI REAL DE LA IMAGEN
        DniData realDataFromOcr = dniParserService.parseDniData(frontText, backText);
        String dniNumber = realDataFromOcr.numeroDni();

        System.out.println("🔍 OCR leyó el DNI: " + dniNumber);

        // 3. BUSCAR EN EL POOL DE PRUEBAS
        if (mockPool.containsKey(dniNumber)) {
            System.out.println("✅ DNI encontrado en el Pool de Pruebas. Retornando data enriquecida...");
            DniData mockData = mockPool.get(dniNumber);

            // Retornamos la data del mock pero podemos incluir los base64 de las imágenes
            // que acabas de tomar para que se vean en el historial de la App
            return new DniData(
                    mockData.numeroDni(), mockData.apellidos(), mockData.nombres(),
                    mockData.fechaNacimiento(), mockData.sexo(), mockData.nacionalidad(),
                    mockData.fechaEmision(), mockData.fechaVencimiento(),
                    mockData.fotoPersona(), // Esta es la foto que usará AWS Rekognition
                    Base64.getEncoder().encodeToString(frontImage.getBytes()),
                    Base64.getEncoder().encodeToString(backImage.getBytes())
            );
        }

        // 4. SI EL DNI NO ESTÁ EN EL POOL, puedes o devolver lo que leyó el OCR
        // o lanzar un error controlado para no ir a RENIEC.
        System.out.println("⚠️ DNI no está en el pool de pruebas. Retornando solo lectura de OCR.");
        return dniProcessingLogic(frontImage, backImage);
    }

    private DniData dniProcessingLogic(MultipartFile front, MultipartFile back) throws Exception {
        String frontText = visionService.extractTextFromImageEnhanced(front);
        String backText = visionService.extractTextFromImageEnhanced(back);
        DniData realDataFromOcr = dniParserService.parseDniData(frontText, backText);
        String dniNumber = realDataFromOcr.numeroDni();

        if (mockPool.containsKey(dniNumber)) {
            DniData mockData = mockPool.get(dniNumber);
            return new DniData(
                    mockData.numeroDni(), mockData.apellidos(), mockData.nombres(),
                    mockData.fechaNacimiento(), mockData.sexo(), mockData.nacionalidad(),
                    mockData.fechaEmision(), mockData.fechaVencimiento(),
                    mockData.fotoPersona(),
                    Base64.getEncoder().encodeToString(front.getBytes()),
                    Base64.getEncoder().encodeToString(back.getBytes())
            );
        }
        return realDataFromOcr;
    }
}