package com.example.RekoDemoBack.controller;

import com.example.RekoDemoBack.DTO.DniData;
import com.example.RekoDemoBack.service.DniMockService;
import com.example.RekoDemoBack.service.DniProcessingService;
import com.example.RekoDemoBack.service.IDniService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dni")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class DniController {

    private final DniProcessingService dniProcessingService;
    //private final DniMockService dniMockService;

    @PostMapping("/process")
    public ResponseEntity<?> processDni(
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage) {

        try {
            // VALIDACIÓN DE ENTRADA
            if (frontImage == null || frontImage.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("La imagen frontal es requerida"));
            }

            if (backImage == null || backImage.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("La imagen trasera es requerida"));
            }

            // VALIDAR TIPO DE IMAGEN
            if (!isValidImageType(frontImage.getContentType())) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "Formato de imagen frontal no válido. Use JPEG, PNG o JPG"
                ));
            }

            if (!isValidImageType(backImage.getContentType())) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "Formato de imagen trasera no válido. Use JPEG, PNG o JPG"
                ));
            }

            // VALIDAR TAMAÑO (máximo 10MB)
            if (frontImage.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "La imagen frontal es demasiado grande. Máximo 10MB"
                ));
            }

            if (backImage.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "La imagen trasera es demasiado grande. Máximo 10MB"
                ));
            }

            System.out.println("\n========================================");
            System.out.println("SOLICITUD DE PROCESAMIENTO RECIBIDA");
            System.out.println("========================================");
            System.out.println("Front image: " + frontImage.getOriginalFilename() +
                    " (" + frontImage.getSize() + " bytes)");
            System.out.println("Back image: " + backImage.getOriginalFilename() +
                    " (" + backImage.getSize() + " bytes)");

            // PROCESAR DNI
            DniData dniData = dniProcessingService.processDni(frontImage, backImage);

            // LOG DE ÉXITO
            System.out.println("\n✅ PROCESAMIENTO COMPLETADO EXITOSAMENTE");
            System.out.println("DNI: " + dniData.numeroDni());
            System.out.println("Nombre: " + dniData.nombres() + " " + dniData.apellidos());
            System.out.println("========================================\n");

            return ResponseEntity.ok(dniData);

        } catch (RuntimeException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            System.err.println("❌ Error interno: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al procesar el DNI. Por favor, intente de nuevo."));
        }
    }

    @PostMapping("/process-test")
    public ResponseEntity<?> processDniMock(
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage) {

        System.out.println("🧪 EJECUTANDO MODO MOCK - SIN CARGOS");
        try {
            DniData mockData = dniProcessingService.processDni(frontImage, backImage);
            return ResponseEntity.ok(mockData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // MÉTODO AUXILIAR PARA VALIDAR TIPO DE IMAGEN
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
                contentType.equalsIgnoreCase("image/jpeg") ||
                        contentType.equalsIgnoreCase("image/png") ||
                        contentType.equalsIgnoreCase("image/jpg")
        );
    }

    // MÉTODO AUXILIAR PARA CREAR RESPUESTAS DE ERROR
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        return error;
    }
}