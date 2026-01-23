package com.example.RekoDemoBack.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageProcessingService {

    /**
     * Extrae la foto del titular con limpieza moderada.
     * Recibe la imagen ya corregida en perspectiva por el AdvancedService.
     */
    public byte[] extractPersonPhoto(BufferedImage documentImage) throws IOException {
        int w = documentImage.getWidth();
        int h = documentImage.getHeight();

        // 1. RECORTE DE PRECISIÓN (Centrado mejorado para DNI Peruano)
        // Usamos proporciones que dejan aire alrededor de la cara para el comparador
        int x = (int) (w * 0.045);     // Ajustado para centrar mejor
        int y = (int) (h * 0.16);      // Un poco más arriba para no cortar frente
        int pWidth = (int) (w * 0.25);  // Ancho estándar de la zona de foto
        int pHeight = (int) (h * 0.56); // Alto estándar

        x = Math.max(0, x);
        y = Math.max(0, y);
        pWidth = Math.min(pWidth, w - x);
        pHeight = Math.min(pHeight, h - y);

        BufferedImage face = documentImage.getSubimage(x, y, pWidth, pHeight);

        // 2. MEJORA NATURAL (Sin quemar la imagen)
        return bufferedImageToByteArray(enhanceFaceNaturally(face));
    }

    private BufferedImage enhanceFaceNaturally(BufferedImage face) {
        int tw = 500;
        int th = 630;
        BufferedImage output = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Fondo blanco neutro
        g.setColor(new Color(245, 245, 245)); // Blanco roto para que no brille tanto
        g.fillRect(0, 0, tw, th);
        g.drawImage(face, 0, 0, tw, th, null);
        g.dispose();

        // Aplicar corrección de contraste suave (Gamma Correction)
        // en lugar de RescaleOp para preservar texturas de piel
        return applyNaturalBalance(output);
    }

    private BufferedImage applyNaturalBalance(BufferedImage img) {
        // Reducimos el ruido de la webcam primero
        float[] matrix = {
                1/16f, 1/8f, 1/16f,
                1/8f, 1/4f, 1/8f,
                1/16f, 1/8f, 1/16f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, matrix));
        BufferedImage denoised = op.filter(img, null);

        // Ajuste manual de niveles para evitar el "filtro blanco"
        for (int y = 0; y < denoised.getHeight(); y++) {
            for (int x = 0; x < denoised.getWidth(); x++) {
                int rgb = denoised.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Aumentamos contraste sin subir el brillo base
                r = (int)(Math.pow(r / 255.0, 0.9) * 255.0);
                g = (int)(Math.pow(g / 255.0, 0.9) * 255.0);
                b = (int)(Math.pow(b / 255.0, 0.9) * 255.0);

                denoised.setRGB(x, y, (clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
            }
        }
        return denoised;
    }

    private int clamp(int val) { return Math.max(0, Math.min(255, val)); }

    public byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
    private BufferedImage enhanceToCarnetStyle(BufferedImage face) {
        // Dimensiones estándar para un carnet de alta calidad
        int tw = 500;
        int th = 650;
        BufferedImage output = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();

        // Calidad máxima de renderizado
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Fondo blanco puro
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, tw, th);
        g.drawImage(face, 0, 0, tw, th, null);
        g.dispose();

        // --- CORRECCIÓN DE COLOR EQUILIBRADA ---
        // Factor 1.15f (contraste suave) | Offset 8f (brillo sutil, antes era 25f y por eso se veía blanco)
        RescaleOp filter = new RescaleOp(1.15f, 8f, null);
        output = filter.filter(output, null);

        // Aplicamos un suavizado suave para reducir el pixelado de la webcam
        return applySoftDenoise(output);
    }

    private BufferedImage applySoftDenoise(BufferedImage image) {
        // Kernel de desenfoque Gaussiano muy leve (mezcla los píxeles pixelados)
        float[] matrix = {
                1/16f, 1/8f, 1/16f,
                1/8f, 1/4f, 1/8f,
                1/16f, 1/8f, 1/16f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, matrix));
        return op.filter(image, null);
    }
}