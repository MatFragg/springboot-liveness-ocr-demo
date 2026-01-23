package com.example.RekoDemoBack.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentAiService {
    String extractTextFromImage(MultipartFile file) throws IOException;;
}
