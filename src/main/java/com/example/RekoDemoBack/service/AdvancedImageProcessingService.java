package com.example.RekoDemoBack.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@AllArgsConstructor
public class AdvancedImageProcessingService {

    private final ImageProcessingService baseImageProcessingService;

    /**
     * Paso intermedio: Detecta el área útil del DNI y delega el recorte del rostro.
     */
    public byte[] extractPersonPhoto(BufferedImage preprocessedImage) throws IOException {
        try {
            // 1. Detectar bordes para eliminar cualquier residuo del fondo (mesa/dedos)
            BufferedImage edges = detectEdges(preprocessedImage);
            Rectangle bounds = findDocumentBounds(edges, preprocessedImage);

            // 2. Obtener el DNI "limpio" (solo la tarjeta)
            BufferedImage correctedDni = preprocessedImage.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);

            // 3. ENCAPSULACIÓN: Pasar el DNI limpio al extractor de rostros carnet
            return baseImageProcessingService.extractPersonPhoto(correctedDni);

        } catch (Exception e) {
            System.err.println("⚠️ Fallo en Advanced, procesando con imagen original: " + e.getMessage());
            return baseImageProcessingService.extractPersonPhoto(preprocessedImage);
        }
    }

    private BufferedImage detectEdges(BufferedImage image) {
        BufferedImage edges = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 1; y < image.getHeight() - 1; y++) {
            for (int x = 1; x < image.getWidth() - 1; x++) {
                // Algoritmo de gradiente simple
                int diff = Math.abs((image.getRGB(x-1, y) & 0xFF) - (image.getRGB(x+1, y) & 0xFF));
                edges.setRGB(x, y, (diff << 16) | (diff << 8) | diff);
            }
        }
        return edges;
    }

    private Rectangle findDocumentBounds(BufferedImage edges, BufferedImage original) {
        // Para imágenes de baja resolución (624x393), el DNI suele ocupar casi todo.
        // Usamos un margen de seguridad de solo el 0.5%
        int marginW = (int) (original.getWidth() * 0.005);
        int marginH = (int) (original.getHeight() * 0.005);
        return new Rectangle(marginW, marginH, original.getWidth() - (2 * marginW), original.getHeight() - (2 * marginH));
    }
}