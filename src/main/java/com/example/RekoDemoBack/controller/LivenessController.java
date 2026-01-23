package com.example.RekoDemoBack.controller;

import com.example.RekoDemoBack.service.RekognitionLivenessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.rekognition.model.GetFaceLivenessSessionResultsResponse;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;
import software.amazon.awssdk.utils.BinaryUtils;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/liveness")
@CrossOrigin("*")
public class LivenessController {

    private final RekognitionLivenessService service;

    public LivenessController(RekognitionLivenessService service) {
        this.service = service;
    }

    @PostMapping("/create-session")
    public ResponseEntity<?> createSession() {
        String sessionId = service.createSession();
        System.out.println("SESIÓN CREADA EN AWS: " + sessionId);
        return ResponseEntity.ok().body(Map.of("sessionId", sessionId));
    }

    @GetMapping("/results/{sessionId}")
    public ResponseEntity<?> getResults(@PathVariable String sessionId) {
        GetFaceLivenessSessionResultsResponse resp = service.getSessionResults(sessionId);

        // 1. Verificación de Estado: Si no tuvo éxito, no es Live
        if (!"SUCCEEDED".equals(resp.statusAsString())) {
            return ResponseEntity.ok(Map.of(
                    "sessionId", resp.sessionId(),
                    "isLive", false,
                    "status", resp.statusAsString(),
                    "message", "La sesión no se completó correctamente"
            ));
        }

        float confidence = resp.confidence() != null ? resp.confidence() : 0.0f;

        // 2. Umbral de seguridad profesional (95% es el estándar recomendado)
        boolean isLive = confidence >= 95.0;

        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("sessionId", resp.sessionId());
        jsonResponse.put("status", resp.statusAsString());
        jsonResponse.put("confidence", confidence);
        jsonResponse.put("isLive", isLive);

        if (resp.referenceImage() != null && resp.referenceImage().bytes() != null) {
            byte[] originalImage = resp.referenceImage().bytes().asByteArray();

            // 🚀 PROCESAMOS A TAMAÑO CARNET AQUÍ
            byte[] passportImage = service.processToPassportSize(originalImage);

            Map<String, Object> referenceImageMap = new HashMap<>();
            referenceImageMap.put("Bytes", BinaryUtils.toBase64(passportImage));
            jsonResponse.put("ReferenceImage", referenceImageMap);
        }

        return ResponseEntity.ok(jsonResponse);
    }

    private Map<String, Object> convertBoundingBox(BoundingBox box) {
        Map<String, Object> boxMap = new HashMap<>();
        boxMap.put("Width", box.width());
        boxMap.put("Height", box.height());
        boxMap.put("Left", box.left());
        boxMap.put("Top", box.top());
        return boxMap;
    }
}