package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.ACJComparisonDTOs;
import com.example.RekoDemoBack.DTO.FaceComparisonResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class ACJApiStrategy implements FaceComparisonStrategy {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ACJAuthService authService;

    @Value("${api.reniec.base-url}")
    private String baseUrl;

    @Value("${api.reniec.channel:API}") // Valor por defecto API
    private String channel;

    public ACJApiStrategy(ACJAuthService authService) {
        this.authService = authService;
    }

    @Override
    public FaceComparisonResult compareFaces(String imageFirst, String imageSecond) {
        // 1. Obtener Token
        String token = authService.getAccessToken();

        // 2. Configurar URL
        String url = baseUrl + "/management/v1/facial-biometrics/compare";

        // 3. Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Importante definir JSON

        // CORRECCIÓN AQUÍ: Agregamos "Bearer " antes del token
        headers.set("Authorization", "Bearer " + token);

        headers.set("channel", channel);

        // 4. Body
        ACJComparisonDTOs.ACJComparisonRequest body =
                new ACJComparisonDTOs.ACJComparisonRequest(imageFirst, imageSecond);

        HttpEntity<ACJComparisonDTOs.ACJComparisonRequest> request = new HttpEntity<>(body, headers);

        FaceComparisonResult result = new FaceComparisonResult();
        result.setProvider(getProviderName());

        try {
            // 5. Ejecutar Petición
            ResponseEntity<ACJComparisonDTOs.ACJComparisonResponse> response =
                    restTemplate.postForEntity(url, request, ACJComparisonDTOs.ACJComparisonResponse.class);

            // 6. Mapear respuesta
            if (response.getBody() != null) {
                ACJComparisonDTOs.ACJComparisonResponse bodyResponse = response.getBody();

                if(bodyResponse.getData() != null) {
                    result.setSimilarityScore(bodyResponse.getData().getSimilarityScore());
                    result.setMatch(bodyResponse.getData().isMatch());
                } else {
                    result.setSimilarityScore(0.0);
                    result.setMatch(false);
                }

                if(bodyResponse.getResult() != null) {
                    result.setResultCode(bodyResponse.getResult().getCode());
                    result.setResultMessage(bodyResponse.getResult().getInfo());
                }
            }
        } catch (HttpClientErrorException e) {
            // Manejo de errores 4xx (como el 401 si persiste)
            System.err.println("Error HTTP RENIEC: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());

            // Si da 401, el token podría haber expirado, invalidamos caché (opcional)
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Aquí podrías implementar lógica para forzar refresco de token
                result.setResultMessage("Error de autenticación con proveedor");
            }
            throw e; // Relanzamos para que el controller lo maneje o devolvemos error controlado
        } catch (Exception e) {
            System.err.println("Error general en RENIEC Strategy: " + e.getMessage());
            throw new RuntimeException("Error al conectar con proveedor de identidad");
        }

        return result;
    }

    @Override
    public String getProviderName() {
        return "reniec";
    }
}