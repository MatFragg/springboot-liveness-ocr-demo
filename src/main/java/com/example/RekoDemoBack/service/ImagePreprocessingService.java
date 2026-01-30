package com.example.RekoDemoBack.service;



import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;



import javax.imageio.ImageIO;

import java.awt.*;

import java.awt.image.BufferedImage;

import java.awt.image.ConvolveOp;

import java.awt.image.Kernel;
import java.awt.image.RescaleOp;

import java.io.ByteArrayInputStream;

import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;



@Service

public class ImagePreprocessingService {

    // Caché de imágenes preprocesadas (hash MD5 -> imagen procesada)
    private final Map<String, CachedImage> imageCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos

    private static class CachedImage {
        byte[] data;
        String filename;
        String contentType;
        long timestamp;

        CachedImage(byte[] data, String filename, String contentType) {
            this.data = data;
            this.filename = filename;
            this.contentType = contentType;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }



    public MultipartFile preprocessImage(MultipartFile originalImage) throws IOException {

        try {
            // Calcular hash MD5 de la imagen original
            String imageHash = calculateMD5(originalImage.getBytes());

            // Verificar caché
            CachedImage cached = imageCache.get(imageHash);
            if (cached != null && !cached.isExpired()) {
                System.out.println("✅ Imagen encontrada en caché (hash: " + imageHash.substring(0, 8) + "...)");
                return new CustomMultipartFile(
                        cached.data,
                        cached.filename,
                        cached.contentType
                );
            }

            // Limpiar caché si está llena
            if (imageCache.size() >= MAX_CACHE_SIZE) {
                cleanExpiredCache();
            }

            System.out.println("🖼️ Preprocesando imagen: " + originalImage.getOriginalFilename());

            System.out.println("📏 Tamaño original: " + originalImage.getSize() + " bytes");



            BufferedImage image = ImageIO.read(originalImage.getInputStream());



            if (image == null) {

                System.err.println("❌ No se pudo leer la imagen original");

                return originalImage;

            }



            System.out.println("📐 Dimensiones originales: " + image.getWidth() + "x" + image.getHeight());



// Aplicar pipeline de mejoras

            BufferedImage enhanced = applyEnhancementPipeline(image);



// Convertir de vuelta a MultipartFile

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ImageIO.write(enhanced, "jpg", baos);

            byte[] bytes = baos.toByteArray();



            System.out.println("✅ Imagen preprocesada - Nuevo tamaño: " + bytes.length + " bytes");

            // Guardar en caché
            String processedFilename = "processed_" + originalImage.getOriginalFilename();
            imageCache.put(imageHash, new CachedImage(bytes, processedFilename, originalImage.getContentType()));
            System.out.println("💾 Imagen guardada en caché (total: " + imageCache.size() + " imágenes)");

            return new CustomMultipartFile(

                    bytes,

                    processedFilename,

                    originalImage.getContentType()

            );



        } catch (Exception e) {

            System.err.println("❌ Error en preprocesamiento: " + e.getMessage());

// Si falla el preprocesamiento, devolver la imagen original

            return originalImage;

        }

    }

    private BufferedImage applyEnhancementPipeline(BufferedImage image) {
        BufferedImage result = image;

        // 1. Super-Resolución OPTIMIZADA (reducida de 2500px a 1600px)
        result = upscaleImage(result, 1600);

        // 2. Limpieza de ruido (DESHABILITADA para mejorar rendimiento)
        // Este filtro es muy costoso (3 bucles anidados sobre millones de píxeles)
        // result = reduceNoise(result);

        // 3. Contraste de bordes (Para el OCR)
        result = enhanceContrast(result);

        // NOTA: adjustBrightness deshabilitado para no quemar el rostro
        // result = adjustBrightness(result);

        // 4. Enfoque
        result = applySharpening(result);

        return result;
    }

    private BufferedImage upscaleImage(BufferedImage image, int targetWidth) {
        double ratio = (double) targetWidth / image.getWidth();
        int targetHeight = (int) (image.getHeight() * ratio);

        BufferedImage upscaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = upscaled.createGraphics();

        // Seteo de máxima calidad para evitar pixelación
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return upscaled;
    }



    private BufferedImage resizeIfNeeded(BufferedImage image, int maxWidth) {

        if (image.getWidth() <= maxWidth) {

            return image;

        }



        double ratio = (double) maxWidth / image.getWidth();

        int newHeight = (int) (image.getHeight() * ratio);



        BufferedImage resized = new BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = resized.createGraphics();



        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);



        g.drawImage(image, 0, 0, maxWidth, newHeight, null);

        g.dispose();



        System.out.println("📐 Imagen redimensionada: " + maxWidth + "x" + newHeight);

        return resized;

    }



    private BufferedImage enhanceContrast(BufferedImage image) {
        // Implementación optimizada usando RescaleOp (operación nativa más rápida)
        // En lugar de iterar pixel por pixel, usamos operaciones vectorizadas

        float scaleFactor = 1.2f; // Factor de contraste
        float offset = -20f;       // Offset para ajustar brillo

        RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);

        BufferedImage contrasted = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );

        rescaleOp.filter(image, contrasted);

        System.out.println("🎨 Contraste mejorado (optimizado con RescaleOp)");
        return contrasted;
    }



    private BufferedImage adjustBrightness(BufferedImage image) {

        BufferedImage brightened = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);



        int brightnessAdjustment = 15; // Aumentar brillo



        for (int y = 0; y < image.getHeight(); y++) {

            for (int x = 0; x < image.getWidth(); x++) {

                int rgb = image.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;

                int g = (rgb >> 8) & 0xFF;

                int b = rgb & 0xFF;



                r = clamp(r + brightnessAdjustment);

                g = clamp(g + brightnessAdjustment);

                b = clamp(b + brightnessAdjustment);



                int newRGB = (r << 16) | (g << 8) | b;

                brightened.setRGB(x, y, newRGB);

            }

        }



        return brightened;

    }



    private BufferedImage applySharpening(BufferedImage image) {

// Kernel de sharpening

        float[] sharpenMatrix = {

                0, -1, 0,

                -1, 5, -1,

                0, -1, 0

        };



        Kernel kernel = new Kernel(3, 3, sharpenMatrix);

        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        return op.filter(image, null);

    }



    private BufferedImage reduceNoise(BufferedImage image) {

// Filtro de mediana simple para reducir ruido

        BufferedImage denoised = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);



        int radius = 1;

        for (int y = radius; y < image.getHeight() - radius; y++) {

            for (int x = radius; x < image.getWidth() - radius; x++) {

                int[] reds = new int[9];

                int[] greens = new int[9];

                int[] blues = new int[9];

                int index = 0;



                for (int dy = -radius; dy <= radius; dy++) {

                    for (int dx = -radius; dx <= radius; dx++) {

                        int rgb = image.getRGB(x + dx, y + dy);

                        reds[index] = (rgb >> 16) & 0xFF;

                        greens[index] = (rgb >> 8) & 0xFF;

                        blues[index] = rgb & 0xFF;

                        index++;

                    }

                }



                int medianRed = getMedian(reds);

                int medianGreen = getMedian(greens);

                int medianBlue = getMedian(blues);



                int newRGB = (medianRed << 16) | (medianGreen << 8) | medianBlue;

                denoised.setRGB(x, y, newRGB);

            }

        }



        return denoised;

    }



    private int getBrightness(int rgb) {

        int r = (rgb >> 16) & 0xFF;

        int g = (rgb >> 8) & 0xFF;

        int b = rgb & 0xFF;

        return (r + g + b) / 3;

    }



    private int getMedian(int[] values) {

// Ordenamiento simple para mediana

        for (int i = 0; i < values.length - 1; i++) {

            for (int j = i + 1; j < values.length; j++) {

                if (values[i] > values[j]) {

                    int temp = values[i];

                    values[i] = values[j];

                    values[j] = temp;

                }

            }

        }

        return values[values.length / 2];

    }



    private int clamp(int value) {

        return Math.max(0, Math.min(255, value));

    }

    private String calculateMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(data.length); // Fallback
        }
    }

    private void cleanExpiredCache() {
        imageCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        System.out.println("🧹 Caché limpiada. Imágenes restantes: " + imageCache.size());
    }



// Clase auxiliar para crear MultipartFile personalizado

    private static class CustomMultipartFile implements MultipartFile {

        private final byte[] content;

        private final String filename;

        private final String contentType;



        public CustomMultipartFile(byte[] content, String filename, String contentType) {

            this.content = content;

            this.filename = filename;

            this.contentType = contentType;

        }



        @Override

        public String getName() { return filename; }



        @Override

        public String getOriginalFilename() { return filename; }



        @Override

        public String getContentType() { return contentType; }



        @Override

        public boolean isEmpty() { return content.length == 0; }



        @Override

        public long getSize() { return content.length; }



        @Override

        public byte[] getBytes() { return content; }



        @Override

        public java.io.InputStream getInputStream() {

            return new ByteArrayInputStream(content);

        }



        @Override

        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {

            java.nio.file.Files.write(dest.toPath(), content);

        }

    }

}