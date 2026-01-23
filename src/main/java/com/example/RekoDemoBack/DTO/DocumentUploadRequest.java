package com.example.RekoDemoBack.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentUploadRequest {
    private String documentType;
    private String base64Image;
}
