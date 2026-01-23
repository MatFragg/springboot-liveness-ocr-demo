package com.example.RekoDemoBack.controller;

import com.example.RekoDemoBack.DTO.CompareFacesResponseDTO;
import com.example.RekoDemoBack.DTO.CompareRequestDTO;
import com.example.RekoDemoBack.DTO.FaceComparisonResult;
import com.example.RekoDemoBack.service.FaceComparisonService;
import com.example.RekoDemoBack.service.RekognitionComparisonService;
import com.example.RekoDemoBack.service.RekognitionLivenessService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.rekognition.model.*;
import java.util.Base64;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/face-comparison")
@CrossOrigin("*")
public class FaceComparisonController {

    private final RekognitionComparisonService comparisonService;
    private final RekognitionLivenessService livenessService;

    private final FaceComparisonService faceComparisonService;

    public FaceComparisonController(RekognitionComparisonService comparisonService,
                                    RekognitionLivenessService livenessService, FaceComparisonService faceComparisonService) {
        this.comparisonService = comparisonService;
        this.livenessService = livenessService;
        this.faceComparisonService = faceComparisonService;
    }

    /**
     * Endpoint 1: Comparar dos imágenes desde archivos
     */
    @PostMapping("/compare-files")
    public ResponseEntity<FaceComparisonResult> compareFacesFromFiles(
            @RequestParam("sourceImage") MultipartFile sourceImage,
            @RequestParam("targetImage") MultipartFile targetImage) {

        try {
            byte[] sourceBytes = sourceImage.getBytes();
            byte[] targetBytes = targetImage.getBytes();

            String base64First = convertToBase64(sourceImage);
            String base64Second = convertToBase64(targetImage);

            FaceComparisonResult result = faceComparisonService.compareFaces(base64First, base64Second);
            /*CompareFacesResponse response = comparisonService.compareFaces(base64First, base64Second);
            CompareFacesResponseDTO result = buildComparisonResponse(response);*/


            System.out.println("Similaridad obtenida: " + result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint 2: Comparar dos imágenes desde Base64
     */
    @PostMapping("/compare-base64")
    public ResponseEntity<?> compareFacesFromBase64(@RequestBody CompareRequestDTO request) {
        try {
            CompareFacesResponse response;

            if (request.getLivenessSessionId() != null && !request.getLivenessSessionId().isEmpty()) {
                // Comparar con imagen de liveness
                response = compareWithLivenessReference(
                        request.getLivenessSessionId(),
                        request.getTargetImageBase64()
                );
            } else {
                // Comparar dos imágenes normales
                response = comparisonService.compareFacesBase64(
                        request.getSourceImageBase64(),
                        request.getTargetImageBase64()
                );
            }

            CompareFacesResponseDTO result = buildComparisonResponse(response);

            if (request.getSimilarityThreshold() != null) {
                result.setIsMatch(result.getSimilarityScore() >= request.getSimilarityThreshold());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error en la comparación: " + e.getMessage(),
                    "details", e.toString()
            ));
        }
    }

    /**
     * Endpoint 3: Comparar con imagen de referencia de liveness
     */
    @PostMapping("/compare-with-liveness")
    public ResponseEntity<?> compareWithLiveness(
            @RequestParam("livenessSessionId") String sessionId,
            @RequestParam("targetImage") MultipartFile targetImage) {

        try {
            CompareFacesResponse response = compareWithLivenessReference(sessionId, targetImage);
            CompareFacesResponseDTO result = new CompareFacesResponseDTO();

            if (!response.faceMatches().isEmpty()) {
                CompareFacesMatch match = response.faceMatches().get(0);
                Float similarity = match.similarity();
                result.setSimilarityScore(similarity);
                result.setIsMatch(similarity > 90.0f);
                result.setConfidenceLevel(getConfidenceLevel(similarity));
                result.setFaceMatchesCount(response.faceMatches().size());

                // Convertir face matches a DTO
                result.setFaceMatches(response.faceMatches().stream()
                        .map(this::convertCompareFacesMatch)
                        .collect(Collectors.toList()));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Endpoint 4: Detectar rostros en una imagen
     */
    @PostMapping("/detect-faces")
    public ResponseEntity<?> detectFaces(@RequestParam("image") MultipartFile image) {
        try {
            byte[] imageBytes = image.getBytes();
            DetectFacesResponse response = comparisonService.detectFaces(imageBytes);

            List<Map<String, Object>> faces = response.faceDetails().stream()
                    .map(this::convertFaceDetail)
                    .collect(Collectors.toList());


            System.out.println(response);
            return ResponseEntity.ok(Map.of(
                    "faceCount", faces.size(),
                    "faces", faces
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error en detección: " + e.getMessage()
            ));
        }
    }

    // ========== MÉTODOS PRIVADOS DE AYUDA ==========

    private CompareFacesResponse compareWithLivenessReference(String sessionId, MultipartFile targetImage)
            throws IOException {
        // Obtener resultados de liveness
        GetFaceLivenessSessionResultsResponse livenessResult =
                livenessService.getSessionResults(sessionId);

        if (livenessResult.referenceImage() == null ||
                livenessResult.referenceImage().bytes() == null) {
            throw new RuntimeException("No hay imagen de referencia en la sesión de liveness");
        }

        // Obtener bytes de la imagen de referencia
        byte[] referenceBytes = livenessResult.referenceImage().bytes().asByteArray();
        byte[] targetBytes = targetImage.getBytes();

        // Comparar
        return comparisonService.compareFaces(referenceBytes, targetBytes);
    }

    private CompareFacesResponse compareWithLivenessReference(String sessionId, String targetBase64) {
        try {
            GetFaceLivenessSessionResultsResponse livenessResult =
                    livenessService.getSessionResults(sessionId);

            if (livenessResult.referenceImage() == null ||
                    livenessResult.referenceImage().bytes() == null) {
                throw new RuntimeException("No hay imagen de referencia en la sesión de liveness");
            }

            byte[] referenceBytes = livenessResult.referenceImage().bytes().asByteArray();
            byte[] targetBytes = decodeBase64(targetBase64);

            return comparisonService.compareFaces(referenceBytes, targetBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error al comparar con liveness: " + e.getMessage(), e);
        }
    }

    private byte[] decodeBase64(String base64String) {
        String cleanBase64;
        if (base64String.contains(",")) {
            cleanBase64 = base64String.substring(base64String.indexOf(",") + 1);
        } else {
            cleanBase64 = base64String;
        }
        return java.util.Base64.getDecoder().decode(cleanBase64);
    }

    private CompareFacesResponseDTO buildComparisonResponse(CompareFacesResponse response) {
        CompareFacesResponseDTO dto = new CompareFacesResponseDTO();

        if (!response.faceMatches().isEmpty()) {
            CompareFacesMatch match = response.faceMatches().get(0);
            Float similarity = match.similarity();

            dto.setStatus("SUCCESS");
            dto.setMessage("Comparación completada exitosamente");
            dto.setSimilarityScore(similarity);
            dto.setIsMatch(similarity > 90.0f);
            dto.setConfidenceLevel(getConfidenceLevel(similarity));
            dto.setFaceMatchesCount(response.faceMatches().size());

            // Convertir face matches
            dto.setFaceMatches(response.faceMatches().stream()
                    .map(this::convertCompareFacesMatch)
                    .collect(Collectors.toList()));

        } else {
            dto.setStatus("NO_MATCH");
            dto.setMessage("No se encontraron coincidencias faciales");
            dto.setSimilarityScore(0.0f);
            dto.setIsMatch(false);
            dto.setConfidenceLevel("BAJA");
            dto.setFaceMatchesCount(0);
        }

        return dto;
    }

    private CompareFacesResponseDTO.FaceMatchDTO convertCompareFacesMatch(CompareFacesMatch compareFacesMatch) {
        CompareFacesResponseDTO.FaceMatchDTO dto = new CompareFacesResponseDTO.FaceMatchDTO();
        dto.setSimilarity(compareFacesMatch.similarity());

        if (compareFacesMatch.face() != null && compareFacesMatch.face().boundingBox() != null) {
            CompareFacesResponseDTO.BoundingBoxDTO boxDto = new CompareFacesResponseDTO.BoundingBoxDTO();
            BoundingBox box = compareFacesMatch.face().boundingBox();
            boxDto.setWidth(box.width());
            boxDto.setHeight(box.height());
            boxDto.setLeft(box.left());
            boxDto.setTop(box.top());
            dto.setBoundingBox(boxDto);
        }

        return dto;
    }

    private Map<String, Object> convertFaceDetail(FaceDetail faceDetail) {
        Map<String, Object> faceMap = new HashMap<>();

        faceMap.put("confidence", faceDetail.confidence());
        if (faceDetail.boundingBox() != null) {
            Map<String, Float> box = new HashMap<>();
            box.put("width", faceDetail.boundingBox().width());
            box.put("height", faceDetail.boundingBox().height());
            box.put("left", faceDetail.boundingBox().left());
            box.put("top", faceDetail.boundingBox().top());
            faceMap.put("boundingBox", box);
        }

        if (faceDetail.ageRange() != null) {
            Map<String, Integer> ageRange = new HashMap<>();
            ageRange.put("low", faceDetail.ageRange().low());
            ageRange.put("high", faceDetail.ageRange().high());
            faceMap.put("ageRange", ageRange);
        }

        if (faceDetail.gender() != null) {
            Map<String, Object> gender = new HashMap<>();
            gender.put("value", faceDetail.gender().valueAsString());
            gender.put("confidence", faceDetail.gender().confidence());
            faceMap.put("gender", gender);
        }

        // Emotions - CORREGIDO
        if (faceDetail.emotions() != null && !faceDetail.emotions().isEmpty()) {
            List<Map<String, Object>> emotions = new ArrayList<>();
            for (Emotion emotion : faceDetail.emotions()) {
                Map<String, Object> emotionMap = new HashMap<>();
                emotionMap.put("type", emotion.typeAsString());
                emotionMap.put("confidence", emotion.confidence());
                emotions.add(emotionMap);
            }
            faceMap.put("emotions", emotions);
        }

        // Pose
        if (faceDetail.pose() != null) {
            Map<String, Float> pose = new HashMap<>();
            pose.put("roll", faceDetail.pose().roll());
            pose.put("yaw", faceDetail.pose().yaw());
            pose.put("pitch", faceDetail.pose().pitch());
            faceMap.put("pose", pose);
        }

        // Quality
        if (faceDetail.quality() != null) {
            Map<String, Float> quality = new HashMap<>();
            quality.put("brightness", faceDetail.quality().brightness());
            quality.put("sharpness", faceDetail.quality().sharpness());
            faceMap.put("quality", quality);
        }

        // Attributes
        Map<String, Boolean> attributes = new HashMap<>();
        if (faceDetail.beard() != null) attributes.put("hasBeard", faceDetail.beard().value());
        if (faceDetail.mustache() != null) attributes.put("hasMustache", faceDetail.mustache().value());
        if (faceDetail.sunglasses() != null) attributes.put("hasSunglasses", faceDetail.sunglasses().value());
        if (faceDetail.eyesOpen() != null) attributes.put("hasEyesOpen", faceDetail.eyesOpen().value());
        if (faceDetail.mouthOpen() != null) attributes.put("hasMouthOpen", faceDetail.mouthOpen().value());

        if (!attributes.isEmpty()) {
            faceMap.put("attributes", attributes);
        }
        System.out.println("============COMPARACION FACIAL REALIZADA=============" );
        return faceMap;
    }

    private String getConfidenceLevel(Float similarity) {
        if (similarity >= 95) return "MUY ALTA";
        if (similarity >= 90) return "ALTA";
        if (similarity >= 80) return "MEDIA";
        if (similarity >= 70) return "BAJA";
        return "MUY BAJA";
    }

    private String convertToBase64(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("El archivo proporcionado es nulo o está vacío.");
        }

        byte[] bytes = file.getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }
}