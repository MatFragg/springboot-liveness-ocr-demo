package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.ACJComparisonDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class ACJAuthService {

    @Value("${api.reniec.base-url}")
    private String baseUrl;

    @Value("${api.reniec.client-id}")
    private String clientId;

    @Value("${api.reniec.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private String cachedToken;

    // Método para obtener el token según
    public String getAccessToken() {
        if (cachedToken != null) return cachedToken;

        String url = baseUrl + "/management/v1/access/token";

        HttpHeaders headers = new HttpHeaders();
        headers.set("channel", "API");

        Map<String, String> body = new HashMap<>();
        body.put("clientId", clientId);
        body.put("clientSecret", clientSecret);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<ACJComparisonDTOs.ACJ_Auth> response = restTemplate.postForEntity(
                url, request, ACJComparisonDTOs.ACJ_Auth.class);

        if (response.getBody() != null) {
            this.cachedToken = response.getBody().getAccessToken();
            return this.cachedToken;
        }
        throw new RuntimeException("No se pudo autenticar con ACJ");
    }
}