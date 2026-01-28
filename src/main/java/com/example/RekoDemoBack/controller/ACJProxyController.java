package com.example.RekoDemoBack.controller;

import com.example.RekoDemoBack.DTO.FaceComparisonResult;
import com.example.RekoDemoBack.DTO.FacialValidationRequest;
import com.example.RekoDemoBack.DTO.FacialValidationResponse;
import com.example.RekoDemoBack.service.ACJAuthService;
import com.example.RekoDemoBack.service.FaceComparisonService;
import com.example.RekoDemoBack.service.ReniecService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/management/v1")
@CrossOrigin(origins = "*")
public class ACJProxyController {

    private final ACJAuthService authService;
    private final FaceComparisonService faceComparisonService;
    private final ReniecService reniecService;

    public ACJProxyController(ACJAuthService authService,
                              FaceComparisonService faceComparisonService, ReniecService reniecService) {
        this.authService = authService;
        this.faceComparisonService = faceComparisonService;
        this.reniecService = reniecService;
    }

    @PostMapping("/access/token")
    public ResponseEntity<?> getToken(@RequestBody Map<String, String> credentials) {
        try {
            String token = authService.getAccessToken();
            return ResponseEntity.ok(Map.of(
                    "accessToken", token,
                    "tokenType", "Bearer"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/facial-biometrics/compare")
    public ResponseEntity<?> compareFaces(@RequestBody CompareProxyRequest request) {
        try {
            FaceComparisonResult result = faceComparisonService.compareFaces(
                    request.getImageFirst(),
                    request.getImageSecond()
            );

            // Formato ACJ
            Map<String, Object> response = Map.of(
                    "result", Map.of(
                            "code", result.getResultCode() != null ? result.getResultCode() : "000",
                            "info", result.getResultMessage() != null ? result.getResultMessage() : "OK"
                    ),
                    "data", Map.of(
                            "similarityScore", result.getSimilarityScore(),
                            "match", result.isMatch()
                    )
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "result", Map.of("code", "ERROR", "info", e.getMessage()),
                            "data", Map.of("similarityScore", 0.0, "match", false)
                    ));
        }
    }

    @PostMapping("/facial-biometrics/capture")
    public ResponseEntity<?> captureValidation(@RequestBody FacialValidationRequest request) {
        try {
            System.out.println("=== Proxy Capture - RENIEC ===");
            System.out.println("DNI: " + request.getDocumentNumber());

            FacialValidationResponse response = reniecService.validateFacial(request);

            // La respuesta ya viene en formato ACJ desde ReniecService
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error en proxy capture: " + e.getMessage());
            FacialValidationResponse errorResponse = new FacialValidationResponse();
            FacialValidationResponse.Result result = new FacialValidationResponse.Result();
            result.setCode("ERROR");
            result.setInfo(e.getMessage());
            errorResponse.setResult(result);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Data
    public static class CompareProxyRequest {
        private String imageFirst;
        private String imageSecond;
    }
}