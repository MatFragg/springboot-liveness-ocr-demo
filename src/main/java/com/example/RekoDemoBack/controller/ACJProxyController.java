package com.example.RekoDemoBack.controller;

import com.example.RekoDemoBack.DTO.FaceComparisonResult;
import com.example.RekoDemoBack.service.ACJAuthService;
import com.example.RekoDemoBack.service.FaceComparisonService;
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

    public ACJProxyController(ACJAuthService authService,
                              FaceComparisonService faceComparisonService) {
        this.authService = authService;
        this.faceComparisonService = faceComparisonService;
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

    @Data
    public static class CompareProxyRequest {
        private String imageFirst;
        private String imageSecond;
    }
}