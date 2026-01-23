package com.example.RekoDemoBack.DTO;

import java.util.Date;

public record DniData(
        String numeroDni,
        String apellidos,
        String nombres,
        String fechaNacimiento,
        String sexo,
        String nacionalidad,
        Date fechaEmision,
        Date fechaVencimiento,
        String fotoPersona,
        String frontImageBase64,
        String backImageBase64
) {}