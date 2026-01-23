package com.example.RekoDemoBack.models;

import com.example.RekoDemoBack.DTO.DniData;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
public class Persona {
    private Long id;
    private String numeroDni;
    private String apellidos;
    private String nombres;
    private String fechaNacimiento;
    private String sexo;
    private String nacionalidad;
    private Date fechaEmision;
    private Date fechaVencimiento;
    private byte[] fotoPersona;
    private byte[] frontImage;
    private byte[] backImage;
    private LocalDateTime fechaCreacion;
    public Persona() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public Persona(DniData data) {
        this.numeroDni = safeTruncate(data.numeroDni(), 8);
        // NUEVO: Manejar campos null
        this.apellidos = data.apellidos() != null ? safeTruncate(data.apellidos(), 100) : null;
        this.nombres = data.nombres() != null ? safeTruncate(data.nombres(), 100) : null;
        this.fechaNacimiento = data.fechaNacimiento() != null ? safeTruncate(data.fechaNacimiento(), 20) : null;
        this.sexo = data.sexo() != null ? safeTruncate(data.sexo(), 20) : null;
        this.nacionalidad = data.nacionalidad() != null ? safeTruncate(data.nacionalidad(), 50) : "PERUANA";
        this.fechaEmision = data.fechaEmision();
        this.fechaVencimiento = data.fechaVencimiento();
        this.fechaCreacion = LocalDateTime.now();

        System.out.println("📋 VALORES A GUARDAR EN BD:");
        System.out.println("   - DNI: " + this.numeroDni);
        System.out.println("   - Apellidos: " + (this.apellidos != null ? this.apellidos : "NULL"));
        System.out.println("   - Nombres: " + (this.nombres != null ? this.nombres : "NULL"));
        System.out.println("   - Sexo: " + (this.sexo != null ? this.sexo : "NULL"));
        System.out.println("   - Nacionalidad: " + this.nacionalidad);
        System.out.println("   - Fecha Nac: " + (this.fechaNacimiento != null ? this.fechaNacimiento : "NULL"));
    }

    private String safeTruncate(String text, int maxLength) {
        if (text == null) {
            return null; // NUEVO: Permitir null
        }

        if (text.equals("NO ENCONTRADO") || text.equals("NO ESPECIFICADO")) {
            return null; // NUEVO: Convertir a null
        }

        if (text.length() > maxLength) {
            String truncated = text.substring(0, maxLength);
            System.out.println("⚠️ TRUNCADO: '" + text + "' -> '" + truncated + "'");
            return truncated;
        }

        return text;
    }
}