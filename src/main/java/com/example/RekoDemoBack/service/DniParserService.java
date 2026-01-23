package com.example.RekoDemoBack.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;

import com.example.RekoDemoBack.DTO.DniData;
import org.springframework.stereotype.Service;


@Service
public class DniParserService {

    private static final Pattern DNI_PATTERN = Pattern.compile("\\b\\d{8}\\b");

    // MÚLTIPLES PATRONES PARA FECHAS PERUANAS
    private static final Pattern FECHA_PERU_PATTERN_1 = Pattern.compile("\\b(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{4})\\b");
    private static final Pattern FECHA_PERU_PATTERN_2 = Pattern.compile("\\b(\\d{2})/(\\d{2})/(\\d{4})\\b");
    private static final Pattern FECHA_PERU_PATTERN_3 = Pattern.compile("\\b(\\d{2})-(\\d{2})-(\\d{4})\\b");
    private static final Pattern FECHA_PERU_PATTERN_4 = Pattern.compile("\\b(\\d{2})\\.?(\\d{2})\\.?(\\d{4})\\b");

    private static final Pattern MRZ_LINE_PATTERN = Pattern.compile("^[A-Z0-9<]{30,}$");

    public DniData parseDniData(String frontText, String backText) {
        System.out.println("\n========================================");
        System.out.println("PROCESANDO DNI PERUANO - VERSIÓN MEJORADA v5");
        System.out.println("========================================\n");

        Map<String, String> extractedData = new HashMap<>();

        String normalizedFront = normalizeText(frontText);
        String normalizedBack = normalizeText(backText);
        // Unimos textos para búsqueda global de MRZ
        String totalText = (frontText + " " + backText).toUpperCase();

        // PRIORIDAD 1: Procesar MRZ
        System.out.println("🎯 PRIORIDAD 1: Procesando MRZ...");
        boolean mrzEncontrado = procesarMRZGlobal(totalText, extractedData);

        // PRIORIDAD 2: Procesar Anverso
        System.out.println("🎯 PRIORIDAD 2: Extrayendo datos complementarios del Anverso...");
        // CORRECCIÓN: Ahora pasamos los 3 argumentos correctamente
        extractFromAnversoMejorado(normalizedFront, extractedData, mrzEncontrado);

        // ESTRATEGIA 3: Backup si aún faltan nombres
        if (isMissingNames(extractedData)) {
            System.out.println("🎯 PRIORIDAD 3: Usando estrategia alternativa de contexto...");
            extractFromAnversoAlternativo(normalizedFront, extractedData);
        }

        // Validación final
        if (!extractedData.containsKey("numeroDni") || extractedData.get("numeroDni").length() != 8) {
            throw new RuntimeException("No se pudo extraer un número de DNI válido");
        }

        removeNotFoundFields(extractedData);
        extractedData.putIfAbsent("nacionalidad", "PERUANA");
        printExtractedData(extractedData);

        return new DniData(
                extractedData.get("numeroDni"),
                extractedData.get("apellidos"),
                extractedData.get("nombres"),
                extractedData.get("fechaNacimiento"),
                extractedData.get("sexo"),
                extractedData.get("nacionalidad"),
                extractedData.containsKey("fechaEmision") ? parseDate(extractedData.get("fechaEmision")) : null,
                extractedData.containsKey("fechaVencimiento") ? parseDate(extractedData.get("fechaVencimiento")) : null,
                null, null, null
        );
    }   

    // Método auxiliar para saber si faltan datos críticos
    private boolean isMissingNames(Map<String, String> data) {
        return !data.containsKey("nombres") || data.get("nombres").isEmpty() ||
                !data.containsKey("apellidos") || data.get("apellidos").isEmpty();
    }

    private boolean procesarMRZGlobal(String text, Map<String, String> data) {
        if (text == null) return false;
        String cleanText = text.replace(" ", "").toUpperCase();

        // 1. Extraer DNI
        Pattern dniPattern = Pattern.compile("I<PER(\\d{8})");
        Matcher dniMatcher = dniPattern.matcher(cleanText);
        if (dniMatcher.find()) {
            data.put("numeroDni", dniMatcher.group(1));
        }

        // 2. Extraer Nombres y Apellidos (Específico para el patrón ALIAGA<<ETHAN<MATIAS)
        Pattern namePattern = Pattern.compile("([A-Z<]+)<<([A-Z<]+)");
        Matcher nameMatcher = namePattern.matcher(cleanText);

        if (nameMatcher.find()) {
            String rawApellidos = nameMatcher.group(1);
            String rawNombres = nameMatcher.group(2);

            // Limpieza: Quitar el código de país y basura de la línea anterior
            String apellidoLimpio = rawApellidos.replaceAll(".*PER", "").replace("<", " ").trim();
            // Quitar caracteres de relleno al final
            String nombreLimpio = rawNombres.replaceAll("<+$", "").replace("<", " ").trim();

            if (!apellidoLimpio.isEmpty()) data.put("apellidos", formatNameFromMrz(apellidoLimpio));
            if (!nombreLimpio.isEmpty()) data.put("nombres", formatNameFromMrz(nombreLimpio));

            System.out.println("✅ Nombres hallados en MRZ: " + nombreLimpio);
        }

        // 3. Sexo
        Pattern sexPattern = Pattern.compile("PER\\d{7}([MF])\\d{7}");
        Matcher sexMatcher = sexPattern.matcher(cleanText);
        if (sexMatcher.find()) {
            data.put("sexo", sexMatcher.group(1).equals("M") ? "MASCULINO" : "FEMENINO");
        }

        return !isMissingNames(data);
    }

    private String normalizeText(String text) {
        if (text == null) return "";

        return text
                .replace("Ñ", "Ñ")
                .replace("ñ", "ñ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void extractFromAnversoMejorado(String frontText, Map<String, String> data, boolean nombrehallado) {
        if (frontText == null || frontText.trim().isEmpty()) return;

        System.out.println("🎯 INICIANDO EXTRACCIÓN MEJORADA DEL ANVERSO");
        if (!data.containsKey("numeroDni")) extractDNIFromText(frontText, data);

        // Pasamos el flag para que no sobrescriba si el MRZ ya funcionó
        extractNombresYApellidosEstrategiaAvanzada(frontText, data, nombrehallado);

        if (!data.containsKey("sexo")) extractSexoFromText(frontText, data);
        extractFechasPeruanasMejorado(frontText, data);
    }

    // NUEVA FUNCIÓN MEJORADA PARA DETECTAR FECHAS EN MÚLTIPLES FORMATOS
    private void extractFechasPeruanasMejorado(String text, Map<String, String> data) {
        System.out.println("\n🔍 BÚSQUEDA EXHAUSTIVA DE FECHAS");
        System.out.println("================================");

        // Lista de todas las fechas encontradas con sus contextos
        List<FechaEncontrada> fechasEncontradas = new ArrayList<>();

        // ESTRATEGIA 1: Buscar con etiquetas específicas
        buscarFechaConEtiqueta(text, "FECHA DE NACIMIENTO", fechasEncontradas, "nacimiento");
        buscarFechaConEtiqueta(text, "NACIMIENTO", fechasEncontradas, "nacimiento");
        buscarFechaConEtiqueta(text, "FECHA NACIMIENTO", fechasEncontradas, "nacimiento");
        buscarFechaConEtiqueta(text, "F.NACIMIENTO", fechasEncontradas, "nacimiento");
        buscarFechaConEtiqueta(text, "F NACIMIENTO", fechasEncontradas, "nacimiento");

        buscarFechaConEtiqueta(text, "FECHA DE EMISIÓN", fechasEncontradas, "emision");
        buscarFechaConEtiqueta(text, "EMISIÓN", fechasEncontradas, "emision");
        buscarFechaConEtiqueta(text, "FECHA EMISIÓN", fechasEncontradas, "emision");

        buscarFechaConEtiqueta(text, "FECHA DE VENCIMIENTO", fechasEncontradas, "vencimiento");
        buscarFechaConEtiqueta(text, "VENCIMIENTO", fechasEncontradas, "vencimiento");
        buscarFechaConEtiqueta(text, "FECHA VENCIMIENTO", fechasEncontradas, "vencimiento");

        // ESTRATEGIA 2: Buscar todas las fechas sin etiqueta y clasificar por posición
        List<String> todasLasFechas = buscarTodasLasFechas(text);

        System.out.println("\n📅 FECHAS DETECTADAS:");
        System.out.println("Total de fechas encontradas: " + todasLasFechas.size());
        for (int i = 0; i < todasLasFechas.size(); i++) {
            System.out.println("  " + (i+1) + ". " + todasLasFechas.get(i));
        }

        // Asignar fechas según prioridad
        asignarFechas(fechasEncontradas, todasLasFechas, data);

        System.out.println("\n✅ RESULTADO FINAL DE FECHAS:");
        System.out.println("  - Fecha Nacimiento: " + data.getOrDefault("fechaNacimiento", "NO ENCONTRADA"));
        System.out.println("  - Fecha Emisión: " + data.getOrDefault("fechaEmision", "NO ENCONTRADA"));
        System.out.println("  - Fecha Vencimiento: " + data.getOrDefault("fechaVencimiento", "NO ENCONTRADA"));
        System.out.println("================================\n");
    }

    private void buscarFechaConEtiqueta(String text, String etiqueta, List<FechaEncontrada> fechas, String tipo) {
        Pattern pattern = Pattern.compile(
                etiqueta + "\\s*[:\\-]?\\s*([0-3]?\\d\\s*[/\\-\\.]?\\s*[0-1]?\\d\\s*[/\\-\\.]?\\s*\\d{4})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String fechaRaw = matcher.group(1).trim();
            String fechaFormateada = parsearYFormatearFecha(fechaRaw);
            if (fechaFormateada != null) {
                fechas.add(new FechaEncontrada(fechaFormateada, tipo, etiqueta));
                System.out.println("✅ Fecha " + tipo + " encontrada con etiqueta '" + etiqueta + "': " + fechaFormateada);
            }
        }
    }

    private List<String> buscarTodasLasFechas(String text) {
        List<String> fechas = new ArrayList<>();

        // Patrón muy flexible para capturar fechas en cualquier formato
        Pattern[] patrones = {
                Pattern.compile("\\b([0-3]?\\d)\\s+([0-1]?\\d)\\s+(\\d{4})\\b"),  // dd mm aaaa
                Pattern.compile("\\b([0-3]?\\d)/([0-1]?\\d)/(\\d{4})\\b"),        // dd/mm/aaaa
                Pattern.compile("\\b([0-3]?\\d)-([0-1]?\\d)-(\\d{4})\\b"),        // dd-mm-aaaa
                Pattern.compile("\\b([0-3]?\\d)\\.([0-1]?\\d)\\.(\\d{4})\\b")     // dd.mm.aaaa
        };

        for (Pattern pattern : patrones) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String dia = matcher.group(1);
                String mes = matcher.group(2);
                String anio = matcher.group(3);

                // Validar que sea una fecha real
                if (esFechaValida(dia, mes, anio)) {
                    String fechaFormateada = String.format("%s-%s-%s",
                            anio,
                            mes.length() == 1 ? "0" + mes : mes,
                            dia.length() == 1 ? "0" + dia : dia
                    );

                    if (!fechas.contains(fechaFormateada)) {
                        fechas.add(fechaFormateada);
                    }
                }
            }
        }

        return fechas;
    }

    private boolean esFechaValida(String dia, String mes, String anio) {
        try {
            int d = Integer.parseInt(dia);
            int m = Integer.parseInt(mes);
            int a = Integer.parseInt(anio);

            // Validaciones básicas
            if (a < 1900 || a > 2100) return false;
            if (m < 1 || m > 12) return false;
            if (d < 1 || d > 31) return false;

            // Validación de días por mes
            int[] diasPorMes = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            if (d > diasPorMes[m - 1]) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String parsearYFormatearFecha(String fechaRaw) {
        // Intentar con todos los patrones posibles
        String[] separadores = {"\\s+", "/", "-", "\\."};

        for (String sep : separadores) {
            String[] partes = fechaRaw.split(sep);
            if (partes.length == 3) {
                try {
                    String dia = partes[0].trim();
                    String mes = partes[1].trim();
                    String anio = partes[2].trim();

                    if (esFechaValida(dia, mes, anio)) {
                        return String.format("%s-%s-%s",
                                anio,
                                mes.length() == 1 ? "0" + mes : mes,
                                dia.length() == 1 ? "0" + dia : dia
                        );
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }

        return null;
    }

    private void asignarFechas(List<FechaEncontrada> fechasConEtiqueta, List<String> todasLasFechas, Map<String, String> data) {
        // Primero asignar las fechas con etiqueta identificada
        for (FechaEncontrada fecha : fechasConEtiqueta) {
            switch (fecha.tipo) {
                case "nacimiento":
                    if (!data.containsKey("fechaNacimiento")) {
                        data.put("fechaNacimiento", fecha.fecha);
                    }
                    break;
                case "emision":
                    if (!data.containsKey("fechaEmision")) {
                        data.put("fechaEmision", fecha.fecha);
                    }
                    break;
                case "vencimiento":
                    if (!data.containsKey("fechaVencimiento")) {
                        data.put("fechaVencimiento", fecha.fecha);
                    }
                    break;
            }
        }

        // Si falta la fecha de nacimiento, intentar inferir de todas las fechas
        if (!data.containsKey("fechaNacimiento") && !todasLasFechas.isEmpty()) {
            // La fecha de nacimiento suele ser la más antigua
            String fechaMasAntigua = todasLasFechas.stream()
                    .filter(f -> {
                        try {
                            int year = Integer.parseInt(f.split("-")[0]);
                            return year >= 1920 && year <= 2010; // Rango razonable para nacimiento
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .min(String::compareTo)
                    .orElse(null);

            if (fechaMasAntigua != null) {
                data.put("fechaNacimiento", fechaMasAntigua);
                System.out.println("🔍 Fecha nacimiento inferida: " + fechaMasAntigua);
            }
        }

        // Si faltan otras fechas, asignar las más recientes
        if (!data.containsKey("fechaEmision") && todasLasFechas.size() >= 2) {
            String fechaMedia = todasLasFechas.stream()
                    .filter(f -> !f.equals(data.get("fechaNacimiento")))
                    .filter(f -> {
                        try {
                            int year = Integer.parseInt(f.split("-")[0]);
                            return year >= 2000 && year <= 2030;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .sorted()
                    .skip(todasLasFechas.size() > 2 ? 1 : 0)
                    .findFirst()
                    .orElse(null);

            if (fechaMedia != null) {
                data.put("fechaEmision", fechaMedia);
            }
        }

        if (!data.containsKey("fechaVencimiento") && todasLasFechas.size() >= 3) {
            String fechaMasReciente = todasLasFechas.stream()
                    .filter(f -> !f.equals(data.get("fechaNacimiento")))
                    .filter(f -> !f.equals(data.get("fechaEmision")))
                    .filter(f -> {
                        try {
                            int year = Integer.parseInt(f.split("-")[0]);
                            return year >= 2020 && year <= 2050;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .max(String::compareTo)
                    .orElse(null);

            if (fechaMasReciente != null) {
                data.put("fechaVencimiento", fechaMasReciente);
            }
        }
    }

    // Clase interna para almacenar fechas encontradas con contexto
    private static class FechaEncontrada {
        String fecha;
        String tipo;
        String etiqueta;

        FechaEncontrada(String fecha, String tipo, String etiqueta) {
            this.fecha = fecha;
            this.tipo = tipo;
            this.etiqueta = etiqueta;
        }
    }

    private void extractNombresYApellidosEstrategiaAvanzada(String text, Map<String, String> data, boolean nombrehallado) {
        // REGLA: Si el MRZ ya trajo nombres limpios, no los tocamos
        if (nombrehallado && data.containsKey("nombres") && data.get("nombres").length() > 2) {
            return;
        }

        String cleanedText = cleanUnwantedWords(text);
        if (extractWithPatterns(cleanedText, data, nombrehallado)) return;
        extractWithLabels(cleanedText, data, nombrehallado);
    }

    private String cleanUnwantedWords(String text) {
        String[] unwantedWords = {
                "SEXO", "CUI", "DNI", "DOCUMENTO", "IDENTIDAD", "NACIONAL",
                "REPÚBLICA", "PERÚ", "REGISTRO", "ESTADO", "CIVIL", "APELLIDOS",
                "NOMBRES", "PRENOMBRES", "PRIMER", "SEGUNDO", "FECHA", "NACIMIENTO",
                "EMISIÓN", "VENCIMIENTO", "LIMA", "PERUANA", "PERUANO"
        };

        String result = text;
        for (String word : unwantedWords) {
            result = result.replaceAll("\\b" + word + "\\b", "");
        }

        return result.replaceAll("\\s+", " ").trim();
    }

    private boolean extractWithPatterns(String text, Map<String, String> data, boolean nombrehallado) {
        System.out.println("🎯 Estrategia 1: Búsqueda por patrones");

        Pattern apellidosPattern = Pattern.compile(
                "(?:Primer\\s+Apellido|PrimerApellido)\\s+([A-Z][a-z]+)\\s+" +
                        "(?:Segundo\\s+Apellido|SegundoApellido)\\s+([A-Z][a-z]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher apellidosMatcher = apellidosPattern.matcher(text);
        if (apellidosMatcher.find()) {
            String primerApellido = apellidosMatcher.group(1);
            String segundoApellido = apellidosMatcher.group(2);
            String apellidosCompletos = primerApellido + " " + segundoApellido;

            data.put("apellidos", apellidosCompletos);
            System.out.println("✅ Apellidos encontrados por patrón: " + apellidosCompletos);

            extractNombresAfterApellidos(text, apellidosMatcher.end(), data, nombrehallado);
            return true;
        }

        return false;
    }

    private void extractNombresAfterApellidos(String text, int startIndex, Map<String, String> data, boolean nombrehallado) {
        String remainingText = text.substring(startIndex);

        Pattern nombresPattern = Pattern.compile(
                "(?:Prenombres|Nombres|Nombres:)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher nombresMatcher = nombresPattern.matcher(remainingText);
        if (nombresMatcher.find()) {
            String nombres = nombresMatcher.group(1);
            if (!nombrehallado) data.put("nombres", nombres);
            System.out.println("✅ Nombres encontrados después de apellidos: " + nombres);
        }
    }

    private boolean extractWithLabels(String text, Map<String, String> data, boolean nombrehallado) {
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toUpperCase();

            // Solo buscar si el campo está vacío
            if (!data.containsKey("apellidos") || data.get("apellidos").isEmpty()) {
                if (line.contains("APEL") && (line.contains("PRIMER") || line.contains("PIRE"))) {
                    String val = extractValueAfterFuzzyLabel(lines[i], "APEL");
                    if (!val.isEmpty()) data.put("apellidos", formatName(val));
                }
            }

            if (!data.containsKey("nombres") || data.get("nombres").isEmpty()) {
                if (line.contains("PRE") || line.contains("NOM")) {
                    String val = extractValueAfterFuzzyLabel(lines[i], "NOM");
                    if (!val.isEmpty()) {
                        val = val.split("(?i)NACIMIENTO")[0].trim();
                        data.put("nombres", formatName(val));
                    }
                }
            }
        }
        return data.containsKey("apellidos") && data.containsKey("nombres");
    }
    private String extractValueAfterFuzzyLabel(String line, String keyword) {
        // Esta regex busca la palabra clave, pero obliga a que haya un espacio o símbolo
        // antes de empezar a capturar el nombre real, evitando capturar el final de la etiqueta.
        Pattern pattern = Pattern.compile(keyword + "[A-Z]*\\s+([A-ZÁÉÍÓÚÑ\\s]{3,})", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(line);
        if (m.find()) {
            String candidate = m.group(1).trim();
            // Si el candidato es otra etiqueta, lo descartamos
            if (candidate.matches("^(PRIMER|SEGUNDO|APELLIDO|NOMBRE|PRENOMBRE).*")) {
                return "";
            }
            return candidate;
        }
        return "";
    }

    private String extractValueAfterLabel(String line, String label) {
        Pattern pattern = Pattern.compile(label + "\\s*[:\\-]?\\s*([A-Za-zÁÉÍÓÚÑáéíóúñ]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private void extractWithContextImproved(String text, Map<String, String> data) {
        System.out.println("🎯 Estrategia 3: Búsqueda contextual mejorada");

        Pattern namePattern = Pattern.compile("\\b([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)(?:\\s+([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)){1,3}\\b");
        Matcher matcher = namePattern.matcher(text);

        List<String> candidates = new ArrayList<>();
        while (matcher.find()) {
            String candidate = matcher.group().trim();

            if (!isCommonWord(candidate) && !containsUnwantedWords(candidate)) {
                candidates.add(candidate);
                System.out.println("🔎 Candidato mejorado: " + candidate);
            }
        }

        if (!candidates.isEmpty()) {
            candidates.sort((a, b) -> Integer.compare(b.length(), a.length()));

            if (candidates.size() >= 2) {
                data.put("apellidos", formatName(candidates.get(0)));
                data.put("nombres", formatName(candidates.get(1)));
                System.out.println("✅ Nombres por contexto mejorado: " + candidates.get(0) + " | " + candidates.get(1));
            } else if (candidates.size() >= 1) {
                String[] parts = candidates.get(0).split("\\s+", 2);
                if (parts.length >= 2) {
                    data.put("apellidos", formatName(parts[0]));
                    data.put("nombres", formatName(parts[1]));
                } else {
                    data.put("apellidos", formatName(candidates.get(0)));
                }
            }
        }
    }

    private boolean containsUnwantedWords(String text) {
        String[] unwanted = {"SEXO", "CUI", "DNI", "FECHA"};
        String upperText = text.toUpperCase();
        for (String word : unwanted) {
            if (upperText.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String formatName(String name) {
        if (name == null || name.isEmpty()) return name;

        String[] words = name.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    private void extractDNIFromText(String text, Map<String, String> data) {
        Matcher matcher = DNI_PATTERN.matcher(text);
        if (matcher.find()) {
            String dni = matcher.group();
            data.put("numeroDni", dni);
            System.out.println("✅ DNI encontrado: " + dni);
        }
    }

    private void extractSexoFromText(String text, Map<String, String> data) {
        if (text.toLowerCase().contains("sexo m") || text.contains(" M ") || text.matches(".*\\bM\\b.*")) {
            data.put("sexo", "MASCULINO");
            System.out.println("✅ Sexo: MASCULINO");
        } else if (text.toLowerCase().contains("sexo f") || text.contains(" F ") || text.matches(".*\\bF\\b.*")) {
            data.put("sexo", "FEMENINO");
            System.out.println("✅ Sexo: FEMENINO");
        }else {
            data.put("sexo","MASCULINO");
            System.out.println("✅ Sexo: MASCULINO");
        }
    }

    private void extractNacionalidadFromText(String text, Map<String, String> data) {
        Pattern nacionalidadPattern = Pattern.compile(
                "(?:NACIONALIDAD|Nacionalidad)\\s*[:\\-]?\\s*([A-Za-zÁÉÍÓÚÑáéíóúñ\\s]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = nacionalidadPattern.matcher(text);
        if (matcher.find()) {
            String nacionalidad = matcher.group(1).trim();
            if (nacionalidad.equalsIgnoreCase("PERUANA") || nacionalidad.equalsIgnoreCase("PERUANO")) {
                data.put("nacionalidad", "PERUANA");
                System.out.println("✅ Nacionalidad encontrada: PERUANA");
            }
        }
    }

    private boolean extractFromMrzMejorado(String backText, Map<String, String> data) {
        if (backText == null || backText.trim().isEmpty()) return false;

        // Buscamos la línea que tiene el formato APELLIDO1<APELLIDO2<<NOMBRES
        // Esta regex captura lo que está antes de << (apellidos) y lo que está después (nombres)
        Pattern mrzNamePattern = Pattern.compile("([A-Z<]+)<<([A-Z<]+)");
        Matcher matcher = mrzNamePattern.matcher(backText.replace(" ", ""));

        if (matcher.find()) {
            // Los apellidos vienen como "ALIAGA<AGUIRRE"
            String apellidosRaw = matcher.group(1);
            String nombresRaw = matcher.group(2);

            // Reemplazamos el < por espacio para tener "ALIAGA AGUIRRE"
            String apellidosCompletos = apellidosRaw.replace("<", " ").trim();
            String nombresCompletos = nombresRaw.replace("<", " ").trim();

            data.put("apellidos", formatNameFromMrz(apellidosCompletos));
            data.put("nombres", formatNameFromMrz(nombresCompletos));

            System.out.println("✅ Datos MRZ extraídos: " + apellidosCompletos + " | " + nombresCompletos);
            return true;
        }
        return false;
    }

    private void extractAdditionalDataFromMrz(String backText, Map<String, String> data) {
        Pattern birthPattern = Pattern.compile("(\\d{2})(\\d{2})(\\d{2})");
        Matcher matcher = birthPattern.matcher(backText);

        if (matcher.find()) {
            String year = matcher.group(1);
            String month = matcher.group(2);
            String day = matcher.group(3);

            int yearInt = Integer.parseInt(year);
            String fullYear = (yearInt > 50 ? "19" : "20") + year;

            String fechaNacimiento = fullYear + "-" + month + "-" + day;
        }

        Pattern nationalityPattern = Pattern.compile("PER|PERUANA|PERU", Pattern.CASE_INSENSITIVE);
        Matcher nationalityMatcher = nationalityPattern.matcher(backText);

        if (nationalityMatcher.find()) {
            String nacionalidad = "PERUANA";
            if (!data.containsKey("nacionalidad") || data.get("nacionalidad").equals("NO ENCONTRADO")) {
                data.put("nacionalidad", nacionalidad);
                System.out.println("✅ Nacionalidad del MRZ: " + nacionalidad);
            }
        } else {
            extractAlternativeNationality(backText, data);
        }
    }

    private void extractAlternativeNationality(String backText, Map<String, String> data) {
        String[] nationalityPatterns = {
                "NACIONALIDAD\\s*[:\\-]?\\s*([A-Z]+)",
                "PAIS\\s*[:\\-]?\\s*([A-Z]+)",
                "NACIONALIDAD\\s*[:\\-]?\\s*(PERUANA|PERUANO)",
                "PERUANA", "PERUANO", "PER"
        };

        for (String pattern : nationalityPatterns) {
            Pattern natPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = natPattern.matcher(backText);
            if (matcher.find()) {
                String nacionalidad = "PERUANA";
                data.put("nacionalidad", nacionalidad);
                System.out.println("✅ Nacionalidad encontrada: " + nacionalidad);
                return;
            }
        }

        if (!data.containsKey("nacionalidad")) {
            data.put("nacionalidad", "PERUANA");
            System.out.println("✅ Nacionalidad por defecto: PERUANA");
        }
    }

    private String formatNameFromMrz(String name) {
        if (name == null || name.isEmpty()) return name;

        String[] words = name.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                if (result.length() > 0) result.append(" ");
                if (word.length() == 1) {
                    result.append(word.toUpperCase());
                } else {
                    result.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase());
                }
            }
        }

        return result.toString();
    }

    private void extractFromAnversoAlternativo(String frontText, Map<String, String> data) {
        System.out.println("🔄 Usando estrategia alternativa para nombres...");

        String[] lines = frontText.split("\n");
        List<String> allWords = new ArrayList<>();

        for (String line : lines) {
            String[] words = line.split("\\s+");
            for (String word : words) {
                if (word.matches("[A-ZÁÉÍÓÚÑ]{3,}")) {
                    allWords.add(word);
                }
            }
        }

        for (int i = 0; i < allWords.size() - 1; i++) {
            String current = allWords.get(i);
            String next = allWords.get(i + 1);

            if (isLikelyLastName(current) && isLikelyFirstName(next)) {
                if (!data.containsKey("apellidos") || data.get("apellidos").equals("NO ENCONTRADO")) {
                    data.put("apellidos", current);
                    System.out.println("✅ Apellido alternativo: " + current);
                }
                if (!data.containsKey("nombres") || data.get("nombres").equals("NO ENCONTRADO")) {
                    data.put("nombres", next);
                    System.out.println("✅ Nombre alternativo: " + next);
                }
                break;
            }
        }
    }

    private boolean isLikelyLastName(String word) {
        String[] commonLastNames = {
                "GARCIA", "RODRIGUEZ", "GONZALEZ", "FERNANDEZ", "LOPEZ", "MARTINEZ",
                "SANCHEZ", "PEREZ", "GOMEZ", "MARTIN", "JIMENEZ", "HERNANDEZ",
                "DIAZ", "MORENO", "MUÑOZ", "ALVAREZ", "ROMERO", "ALONSO", "GUTIERREZ"
        };

        String upperWord = word.toUpperCase();
        for (String lastName : commonLastNames) {
            if (upperWord.equals(lastName)) return true;
        }

        return word.length() >= 4 && word.matches("[A-ZÁÉÍÓÚÑ]+");
    }

    private boolean isLikelyFirstName(String word) {
        String[] commonFirstNames = {
                "JUAN", "JOSE", "LUIS", "CARLOS", "JORGE", "MANUEL", "PEDRO",
                "MARIA", "ANA", "ROSA", "ELENA", "LUISA", "CARMEN", "TERESA",
                "MIGUEL", "ANDRES", "RAFAEL", "FRANCISCO", "ANTONIO", "DAVID"
        };

        String upperWord = word.toUpperCase();
        for (String firstName : commonFirstNames) {
            if (upperWord.equals(firstName)) return true;
        }

        return word.length() >= 3 && word.matches("[A-ZÁÉÍÓÚÑ]+");
    }

    private Date parseDate(String dateStr) {
        try {
            if (dateStr != null && !dateStr.equals("NO ENCONTRADO")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(dateStr);
            }
        } catch (Exception e) {
            System.err.println("❌ Error parseando fecha: " + dateStr);
        }
        return null;
    }

    private void removeNotFoundFields(Map<String, String> data) {
        String[] fieldsToCheck = {"apellidos", "nombres", "fechaNacimiento", "sexo", "nacionalidad"};

        for (String field : fieldsToCheck) {
            if (data.containsKey(field) && "NO ENCONTRADO".equals(data.get(field))) {
                data.remove(field);
                System.out.println("🗑️ Removido campo '" + field + "' por tener valor 'NO ENCONTRADO'");
            }
        }
    }

    private void printExtractedData(Map<String, String> data) {
        System.out.println("\n========================================");
        System.out.println("RESUMEN DE DATOS EXTRAÍDOS");
        System.out.println("========================================");
        data.forEach((key, value) ->
                System.out.println(String.format("%-20s: %s", key, value))
        );
        System.out.println("========================================\n");
    }

    private boolean isCommonWord(String word) {
        String[] commonWords = {
                "República", "Perú", "Registro", "Nacional", "Identificación",
                "Estado", "Civil", "Documento", "Identidad", "Duplicado",
                "Sexo", "Fecha", "Nacimiento", "Emisión", "Vencimiento"
        };

        String lowerWord = word.toLowerCase();
        for (String common : commonWords) {
            if (lowerWord.contains(common.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}