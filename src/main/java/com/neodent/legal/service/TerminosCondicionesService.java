package com.neodent.legal.service;

import com.neodent.legal.dto.TerminosCondicionesResponse;
import com.neodent.legal.model.TerminosCondiciones;
import com.neodent.legal.repository.TerminosCondicionesRepository;
import com.neodent.shared.exception.BusinessException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminosCondicionesService {

    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String NOMBRE_ARCHIVO_DEFECTO = "terminos-y-condiciones.pdf";
    public static final String CLAVE_S3_DEFECTO = "documentos/legales/terminos-y-condiciones.pdf";
    private static final long MAX_BYTES = 10 * 1024 * 1024; // 10 MB

    private final TerminosCondicionesRepository repository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<TerminosCondicionesResponse> listarTodos() {
        return repository.findAllByOrderByFechaCreacionDesc().stream()
            .map(this::mapearResponse)
            .toList();
    }

    @Transactional
    public TerminosCondicionesResponse subirYCrear(MultipartFile archivo, String titulo, String version, boolean activar) {
        validarArchivoPdf(archivo);

        String ver = (version == null || version.isBlank()) ? "v1.0" : version.trim();
        if (repository.existsByVersion(ver)) {
            throw new BusinessException("Ya existe una versión registrada con el código: " + ver);
        }

        String tit = (titulo == null || titulo.isBlank()) ? ("Términos y Condiciones " + ver) : titulo.trim();
        String originalFilename = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : NOMBRE_ARCHIVO_DEFECTO;
        String sanitizedFilename = sanitizarNombre(originalFilename);

        String claveS3 = "documentos/legales/" + System.currentTimeMillis() + "_" + ver.replace(" ", "_") + "_" + sanitizedFilename;

        try {
            byte[] bytes = archivo.getBytes();
            storageService.guardar(claveS3, bytes, CONTENT_TYPE_PDF);
            log.info("Archivo de términos subido a S3 con clave: {}", claveS3);

            if (activar) {
                desactivarTodas();
            }

            TerminosCondiciones doc = new TerminosCondiciones();
            doc.setTitulo(tit);
            doc.setVersion(ver);
            doc.setContenido(claveS3);
            doc.setFechaPublicacion(LocalDateTime.now());
            doc.setVigente(activar);

            TerminosCondiciones guardado = repository.save(doc);
            return mapearResponse(guardado);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Error al leer archivo PDF", ex);
            throw new BusinessException("No se pudo leer el archivo PDF seleccionado: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Error al almacenar documento en S3 con clave {}: {}", claveS3, ex.getMessage(), ex);
            throw new BusinessException("No se pudo guardar el archivo en S3: " + ex.getMessage());
        }
    }

    private void desactivarTodas() {
        try {
            List<TerminosCondiciones> todas = repository.findAll();
            for (TerminosCondiciones t : todas) {
                if (Boolean.TRUE.equals(t.getVigente())) {
                    t.setVigente(false);
                }
            }
            repository.saveAll(todas);
        } catch (Exception ex) {
            log.warn("Fallo al desactivar versiones anteriores: {}", ex.getMessage());
        }
    }

    @Transactional
    public TerminosCondicionesResponse cambiarEstado(Long id, boolean activo) {
        TerminosCondiciones doc = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Documento de términos no encontrado"));

        if (activo) {
            desactivarTodas();
            doc.setVigente(true);
        } else {
            doc.setVigente(false);
        }

        return mapearResponse(repository.save(doc));
    }

    @Transactional
    public void eliminar(Long id) {
        TerminosCondiciones doc = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Documento de términos no encontrado"));

        try {
            if (doc.getContenido() != null && doc.getContenido().startsWith("documentos/")) {
                if (storageService.existe(doc.getContenido())) {
                    storageService.eliminar(doc.getContenido());
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo eliminar archivo de S3: {}", ex.getMessage());
        }

        repository.delete(doc);
    }

    @Transactional(readOnly = true)
    public byte[] obtenerPdfPorId(Long id) {
        TerminosCondiciones doc = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Documento de términos no encontrado"));

        return resolverBytesDocumento(doc);
    }

    @Transactional(readOnly = true)
    public byte[] obtenerPdfActivo() {
        var opt = repository.findFirstByVigenteTrueOrderByFechaPublicacionDesc();

        if (opt.isPresent()) {
            try {
                return resolverBytesDocumento(opt.get());
            } catch (Exception ex) {
                log.warn("Fallo al obtener versión activa desde S3: {}", ex.getMessage());
            }
        }

        // Fallback a clave por defecto en S3
        if (storageService.existe(CLAVE_S3_DEFECTO)) {
            try {
                return storageService.obtener(CLAVE_S3_DEFECTO);
            } catch (Exception ignored) {}
        }

        // Último fallback: texto plano si la BD solo tiene texto
        if (opt.isPresent() && opt.get().getContenido() != null) {
            return opt.get().getContenido().getBytes(StandardCharsets.UTF_8);
        }

        throw new ResourceNotFoundException("No hay documento de términos y condiciones disponible actualmente");
    }

    @Transactional(readOnly = true)
    public TerminosCondicionesResponse obtenerInfoActivo() {
        var opt = repository.findFirstByVigenteTrueOrderByFechaPublicacionDesc();
        if (opt.isPresent()) {
            return mapearResponse(opt.get());
        }

        return new TerminosCondicionesResponse(
            null,
            "Términos y Condiciones de Uso de NeoDent",
            "1.0",
            NOMBRE_ARCHIVO_DEFECTO,
            CLAVE_S3_DEFECTO,
            0L,
            true,
            "/api/terminos-condiciones",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    private byte[] resolverBytesDocumento(TerminosCondiciones doc) {
        String contenido = doc.getContenido();
        if (contenido != null && storageService.existe(contenido)) {
            return storageService.obtener(contenido);
        }
        if (storageService.existe(CLAVE_S3_DEFECTO)) {
            return storageService.obtener(CLAVE_S3_DEFECTO);
        }
        if (contenido != null) {
            return contenido.getBytes(StandardCharsets.UTF_8);
        }
        throw new ResourceNotFoundException("Archivo de términos no encontrado en almacenamiento");
    }

    private TerminosCondicionesResponse mapearResponse(TerminosCondiciones doc) {
        String clave = doc.getContenido() != null ? doc.getContenido() : CLAVE_S3_DEFECTO;
        String nombreArchivo = clave.contains("/") ? clave.substring(clave.lastIndexOf('/') + 1) : NOMBRE_ARCHIVO_DEFECTO;

        return new TerminosCondicionesResponse(
            doc.getId(),
            doc.getTitulo(),
            doc.getVersion(),
            nombreArchivo,
            clave,
            0L,
            Boolean.TRUE.equals(doc.getVigente()),
            "/api/terminos-condiciones/admin/" + doc.getId() + "/descargar",
            doc.getFechaPublicacion(),
            doc.getFechaCreacion() != null ? doc.getFechaCreacion() : doc.getFechaPublicacion()
        );
    }

    private void validarArchivoPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Debes seleccionar un archivo PDF.");
        }

        if (archivo.getSize() > MAX_BYTES) {
            throw new BusinessException("El documento no puede superar los 10 MB.");
        }

        String contentType = archivo.getContentType();
        String originalName = archivo.getOriginalFilename();

        boolean esPdf = (contentType != null && contentType.equalsIgnoreCase(CONTENT_TYPE_PDF))
            || (originalName != null && originalName.toLowerCase().endsWith(".pdf"));

        if (!esPdf) {
            throw new BusinessException("El archivo debe estar en formato PDF.");
        }
    }

    private String sanitizarNombre(String nombre) {
        String normalizado = Normalizer.normalize(nombre, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .replaceAll("[^a-zA-Z0-9._-]", "_");
        return normalizado.toLowerCase();
    }
}
