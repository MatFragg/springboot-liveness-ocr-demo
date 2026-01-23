package com.example.RekoDemoBack.service;



import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;



import javax.imageio.ImageIO;

import java.awt.*;

import java.awt.image.BufferedImage;

import java.awt.image.ConvolveOp;

import java.awt.image.Kernel;

import java.io.ByteArrayInputStream;

import java.io.ByteArrayOutputStream;

import java.io.IOException;



@Service

public class ImagePreprocessingService {



    public MultipartFile preprocessImage(MultipartFile originalImage) throws IOException {

        try {

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



            return new CustomMultipartFile(

                    bytes,

                    "processed_" + originalImage.getOriginalFilename(),

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

        // 1. Super-Resolución
        result = upscaleImage(result, 2500);

        // 2. Limpieza de ruido (Crucial para webcams)
        result = reduceNoise(result);

        // 3. Contraste de bordes (Para el OCR)
        result = enhanceContrast(result);

        // NOTA: Quitamos adjustBrightness de aquí para no quemar el rostro
        // result = adjustBrightness(result); // <-- ELIMINAR O COMENTAR ESTA LÍNEA

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

        BufferedImage contrasted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);



// Encontrar min y max valores de brillo

        int min = 255, max = 0;

        for (int y = 0; y < image.getHeight(); y++) {

            for (int x = 0; x < image.getWidth(); x++) {

                int rgb = image.getRGB(x, y);

                int brightness = getBrightness(rgb);

                if (brightness < min) min = brightness;

                if (brightness > max) max = brightness;

            }

        }



// Aplicar stretch contrast

        double scale = 255.0 / (max - min);

        for (int y = 0; y < image.getHeight(); y++) {

            for (int x = 0; x < image.getWidth(); x++) {

                int rgb = image.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;

                int g = (rgb >> 8) & 0xFF;

                int b = rgb & 0xFF;



                r = (int) ((r - min) * scale);

                g = (int) ((g - min) * scale);

                b = (int) ((b - min) * scale);



                r = clamp(r);

                g = clamp(g);

                b = clamp(b);



                int newRGB = (r << 16) | (g << 8) | b;

                contrasted.setRGB(x, y, newRGB);

            }

        }



        System.out.println("🎨 Contraste mejorado: min=" + min + ", max=" + max);

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