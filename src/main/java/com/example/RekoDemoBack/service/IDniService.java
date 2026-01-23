package com.example.RekoDemoBack.service;

import com.example.RekoDemoBack.DTO.DniData;
import org.springframework.web.multipart.MultipartFile;

public interface IDniService {
    DniData processDni(MultipartFile frontImage, MultipartFile backImage) throws Exception;
}