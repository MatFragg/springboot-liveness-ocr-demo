package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.ACJ_Auth;
import com.example.RekoDemoBack.DTO.FacialValidationRequest;
import com.example.RekoDemoBack.DTO.FacialValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.sql.Date;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReniecService {

    @Value("${api.reniec.base-url}")
    private String baseUrl;

    @Value("${api.reniec.client-id}")
    private String clientId;

    @Value("${api.reniec.client-secret}")
    private String clientSecret;

    @Value("${api.reniec.channel}")
    private String channel;

    @Value("${api.reniec.mock-enabled:false}")
    private boolean mockEnabled;

    private String token;
    private Long tokenExpirationTime;

    private final RestTemplate restTemplate;

    public ReniecService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Paso 1: Obtener el Token de Acceso
     */
    private synchronized String getAccessToken() {
        // SIEMPRE obtener nuevo token - no confiar en cache
        return fetchNewTokenWithoutCache();
    }

    private String fetchNewTokenWithoutCache() {
        String url = baseUrl + "/management/v1/access/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("channel", "API"); // Usar literal en vez de variable
        Map<String, String> body = new HashMap<>();
        body.put("clientId", clientId);
        body.put("clientSecret", clientSecret);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ACJ_Auth> response = restTemplate.postForEntity(url, request, ACJ_Auth.class);

            if (response.getBody() != null && response.getBody().getAccessToken() != null) {
                String newToken = response.getBody().getAccessToken();

                // DEBUG: Mostrar token (primeros y últimos chars)
                String tokenPreview = newToken.length() > 30
                        ? newToken.substring(0, 15) + "..." + newToken.substring(newToken.length() - 15)
                        : newToken;
                System.out.println("🔓 NUEVO TOKEN: " + tokenPreview);

                // Reset cache
                this.token = newToken;
                this.tokenExpirationTime = System.currentTimeMillis() + 30000; // 30 segundos por seguridad

                return newToken;
            }
        } catch (Exception e) {
            System.err.println("❌ Error crítico obteniendo token: " + e.getMessage());
        }

        throw new RuntimeException("No se pudo obtener token válido");
    }

    public FacialValidationResponse validateFacial(FacialValidationRequest request) {
        // Validación de flag Mock
        if (mockEnabled) {
            System.out.println("⚠️ MODO MOCK ACTIVO: Simulando respuesta de RENIEC para DNI " + request.getDocumentNumber());
            return getMockResponse(request);
        }

        // Si no es mock, llamamos a la API real
        return callRealReniecApi(request);
    }

    private FacialValidationResponse callRealReniecApi(FacialValidationRequest request) {
        System.out.println("\n=== INICIANDO VALIDACIÓN FACIAL REAL ===");
        System.out.println("DNI: " + request.getDocumentNumber());
        System.out.println("Serial Number: " + request.getSerialNumber());

        String currentToken;
        try {
            currentToken = getAccessToken();
            if (currentToken == null || currentToken.trim().isEmpty()) {
                throw new RuntimeException("Token vacío o nulo");
            }
            System.out.println("✅ Token disponible");
        } catch (Exception e) {
            System.err.println("❌ Error crítico obteniendo token: " + e.getMessage());
            FacialValidationResponse errorResponse = new FacialValidationResponse();
            FacialValidationResponse.Result result = new FacialValidationResponse.Result();
            result.setCode("ERROR_TOKEN");
            result.setInfo("No se pudo obtener token de acceso: " + e.getMessage());
            errorResponse.setResult(result);
            return errorResponse;
        }

        String url = baseUrl + "/management/v1/facial-biometrics/capture";
        System.out.println("🌐 URL de validación: " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + currentToken);
        headers.set("channel", channel);

        System.out.println("📨 Headers configurados:");
        System.out.println("   Content-Type: " + headers.getContentType());
        System.out.println("   Authorization: [TOKEN_PRESENTE]");
        System.out.println("   channel: " + headers.get("channel"));

        HttpEntity<FacialValidationRequest> requestEntity = new HttpEntity<>(request, headers);

        System.out.println("📤 Request Body (resumido):");
        System.out.println("   documentNumber: " + request.getDocumentNumber());
        System.out.println("   serialNumber: " + request.getSerialNumber());
        System.out.println("   type: " + request.getType());
        System.out.println("   quality: " + request.getQuality());
        System.out.println("   template length: " + (request.getTemplate() != null ? request.getTemplate().length() : 0) + " chars");

        try {
            System.out.println("🔄 Enviando request a API RENIEC...");

            ResponseEntity<FacialValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    FacialValidationResponse.class
            );

            System.out.println("📥 Respuesta recibida:");
            System.out.println("   Status: " + response.getStatusCode());
            System.out.println("   Headers: " + response.getHeaders());

            if (response.getBody() != null) {
                System.out.println("   Result Code: " +
                        (response.getBody().getResult() != null ?
                                response.getBody().getResult().getCode() : "null"));
                System.out.println("   Result Info: " +
                        (response.getBody().getResult() != null ?
                                response.getBody().getResult().getInfo() : "null"));
            }

            return response.getBody();

        } catch (HttpClientErrorException e) {
            System.err.println("❌ Error HTTP en validación facial:");
            System.err.println("   Status: " + e.getStatusCode());
            System.err.println("   Response Body: " + e.getResponseBodyAsString());

            if (e.getResponseHeaders() != null) {
                System.err.println("   Response Headers: " + e.getResponseHeaders());
            }

            FacialValidationResponse errorResponse = new FacialValidationResponse();
            FacialValidationResponse.Result result = new FacialValidationResponse.Result();

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                result.setCode("ERROR_AUTH");
                result.setInfo("Token expirado o inválido. Body: " + e.getResponseBodyAsString());
            } else {
                result.setCode("ERROR_" + e.getStatusCode().value());
                result.setInfo("Error HTTP: " + e.getStatusText() + ". Body: " + e.getResponseBodyAsString());
            }

            errorResponse.setResult(result);
            return errorResponse;
        }
    }

    private FacialValidationResponse getMockResponse(FacialValidationRequest request) {
        FacialValidationResponse response = new FacialValidationResponse();
        FacialValidationResponse.Result result = new FacialValidationResponse.Result();
        FacialValidationResponse.FacialValidationData data = new FacialValidationResponse.FacialValidationData();

        String dni = request.getDocumentNumber();

        // 0000 indica que la petición HTTP fue exitosa, el detalle de negocio va en 'data'
        result.setCode("0000");
        result.setInfo("La respuesta es satisfactoria.");

        // SIMULACIÓN DE ESCENARIOS

        // Escenario 1: HIT (Éxito) - DNI Real o código de prueba
        if ("72838997".equals(dni) || "70006000".equals(dni)) {
            data.setReniecErrorCode(70006); // CÓDIGO HIT
            data.setReniecErrorDescription("HIT: Persona Identificada");

            data.setDocumentType(1);
            data.setDocumentNumber(dni);
            data.setPersonName("ETHAN MATIAS");
            data.setPersonLastName("ALIAGA");
            data.setPersonMotherLastName("AGUIRRE");
            data.setExpirationDate("16-02-2032");
            data.setValidity("1");
            data.setRestriction("");
            data.setRestrictionGroup("22");
            data.setTraking("mock-" + java.util.UUID.randomUUID().toString()); // Tracking falso

            response.setResult(result);
            response.setData(data);
        }
        // Escenario 2: NO HIT (Rostro no coincide)
        else if ("70007000".equals(dni)) {
            data.setReniecErrorCode(70007);
            data.setReniecErrorDescription("NO HIT: Rostro no corresponde");

            data.setDocumentNumber(dni);
            data.setTraking("mock-nohit-" + java.util.UUID.randomUUID().toString());

            response.setResult(result);
            response.setData(data);
        }
        // CASO 3: EL USUARIO NO EXISTE (2) - Default para cualquier otro DNI
        else {
            data.setReniecErrorCode(2);
            data.setReniecErrorDescription("El usuario no existe");

            data.setDocumentNumber(dni);
            data.setTraking("mock-error-" + java.util.UUID.randomUUID().toString());

            response.setResult(result);
            response.setData(data);
        }

        // Delay para realismo (simula latencia de red)
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        return response;
    }
}