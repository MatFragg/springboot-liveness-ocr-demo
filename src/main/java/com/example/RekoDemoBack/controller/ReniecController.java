package com.example.RekoDemoBack.controller;

import com.example.RekoDemoBack.DTO.FacialValidationRequest;
import com.example.RekoDemoBack.DTO.FacialValidationResponse;
import com.example.RekoDemoBack.service.ReniecService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/management")
@CrossOrigin(origins = "http://localhost:4200")
public class ReniecController {
    private final ReniecService reniecService;

    public ReniecController(ReniecService reniecService) {
        this.reniecService = reniecService;
    }

    @PostMapping("/v1/facial-biometrics/capture/try")
    public ResponseEntity<?> validacionFacial(@RequestBody FacialValidationRequest request) {
        try {
            System.out.println("=== Inicio Validación Facial ===");
            System.out.println("DNI recibido: " + request.getDocumentNumber());
            System.out.println("Serial Number: " + request.getSerialNumber());
            System.out.println("Tipo: " + request.getType());

            // NO modificar quality - usar el valor que viene del frontend
            System.out.println("Calidad: " + request.getQuality());

            // Formatear documentNumber CON "/" al inicio (como en la documentación)
            String documentNumber = request.getDocumentNumber();
            request.setDocumentNumber(documentNumber);
            System.out.println("DNI formateado: " + documentNumber);

            FacialValidationResponse response = reniecService.validateFacial(request);

            // Verificamos si la respuesta interna del API fue exitosa
            if (response.getResult() != null && response.getResult().getCode() != null) {
                String code = response.getResult().getCode();

                // Aceptar tanto "000" como "0000" como códigos de éxito
                if ("000".equals(code) || "0000".equals(code)) {
                    System.out.println("✓ HIT - Validación exitosa. Código: " + code);
                    System.out.println("=== Fin Validación Facial (Éxito) ===");
                    return ResponseEntity.ok(response.getData());
                } else {
                    System.out.println("✗ NO HIT - Código de error: " + code);
                    System.out.println("Mensaje: " + response.getResult().getInfo());

                    // Verificar si hay datos adicionales en la respuesta
                    if (response.getData() != null) {
                        System.out.println("Código RENIEC: " + response.getData().getReniecErrorCode());
                        System.out.println("Descripción: " + response.getData().getReniecErrorDescription());
                    }

                    System.out.println("=== Fin Validación Facial (Error) ===");
                    return ResponseEntity.badRequest().body(response.getResult());
                }
            } else {
                System.out.println("✗ Respuesta sin resultado válido");
                System.out.println("=== Fin Validación Facial (Error) ===");
                return ResponseEntity.badRequest()
                        .body("{\"code\":\"ERROR_RESPONSE\",\"info\":\"Respuesta inválida del servicio\"}");
            }
        } catch (Exception e) {
            System.err.println("✗ Error en validación facial: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=== Fin Validación Facial (Excepción) ===");
            return ResponseEntity.internalServerError()
                    .body("{\"code\":\"ERROR_INTERNAL\",\"info\":\"Error interno: " + e.getMessage() + "\"}");
        }
    }
}