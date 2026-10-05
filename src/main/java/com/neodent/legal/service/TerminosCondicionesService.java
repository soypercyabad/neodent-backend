package com.neodent.legal.service;

import com.neodent.legal.dto.AceptacionTerminosResponse;
import com.neodent.legal.dto.TerminosCondicionesResponse;
import com.neodent.legal.model.AceptacionTerminos;
import com.neodent.legal.model.TerminosCondiciones;
import com.neodent.legal.repository.AceptacionTerminosRepository;
import com.neodent.legal.repository.TerminosCondicionesRepository;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.personal.model.Personal;
import com.neodent.personal.repository.PersonalRepository;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminosCondicionesService {

    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String NOMBRE_ARCHIVO_DEFECTO = "terminos-y-condiciones.pdf";
    public static final String CLAVE_S3_DEFECTO = "documentos/legales/terminos-y-condiciones.pdf";
    private static final long MAX_BYTES = 10 * 1024 * 1024; // 10 MB

    private final TerminosCondicionesRepository repository;
    private final AceptacionTerminosRepository aceptacionRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
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

    @Transactional
    public TerminosCondicionesResponse editar(Long id, String titulo, String version, MultipartFile nuevoArchivo, Boolean activar) {
        TerminosCondiciones doc = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Documento de términos no encontrado"));

        String ver = (version == null || version.isBlank()) ? doc.getVersion() : version.trim();
        if (!ver.equalsIgnoreCase(doc.getVersion()) && repository.existsByVersion(ver)) {
            throw new BusinessException("Ya existe otra versión registrada con el código: " + ver);
        }

        String tit = (titulo == null || titulo.isBlank()) ? doc.getTitulo() : titulo.trim();
        doc.setTitulo(tit);
        doc.setVersion(ver);

        // Si se seleccionó un nuevo archivo PDF, se reemplaza en S3
        if (nuevoArchivo != null && !nuevoArchivo.isEmpty()) {
            validarArchivoPdf(nuevoArchivo);
            String originalFilename = nuevoArchivo.getOriginalFilename() != null ? nuevoArchivo.getOriginalFilename() : NOMBRE_ARCHIVO_DEFECTO;
            String sanitizedFilename = sanitizarNombre(originalFilename);
            String nuevaClaveS3 = "documentos/legales/" + System.currentTimeMillis() + "_" + ver.replace(" ", "_") + "_" + sanitizedFilename;

            try {
                byte[] bytes = nuevoArchivo.getBytes();
                storageService.guardar(nuevaClaveS3, bytes, CONTENT_TYPE_PDF);

                // Intentar eliminar el anterior si era un archivo en la misma carpeta
                if (doc.getContenido() != null && doc.getContenido().startsWith("documentos/legales/")) {
                    try {
                        storageService.eliminar(doc.getContenido());
                    } catch (Exception ignored) {}
                }

                doc.setContenido(nuevaClaveS3);
            } catch (IOException ex) {
                throw new BusinessException("No se pudo leer el nuevo archivo PDF: " + ex.getMessage());
            } catch (Exception ex) {
                throw new BusinessException("No se pudo actualizar el archivo en S3: " + ex.getMessage());
            }
        }

        if (Boolean.TRUE.equals(activar) && !Boolean.TRUE.equals(doc.getVigente())) {
            desactivarTodas();
            doc.setVigente(true);
        }

        return mapearResponse(repository.save(doc));
    }

    @Transactional
    public TerminosCondicionesResponse cambiarEstado(Long id, boolean activo) {
        TerminosCondiciones doc = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Documento de términos no encontrado"));

        if (!activo && Boolean.TRUE.equals(doc.getVigente())) {
            throw new BusinessException("No se puede desactivar directamente la versión oficial. Para cambiarla, activa otra versión.");
        }

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

        if (Boolean.TRUE.equals(doc.getVigente())) {
            throw new BusinessException("No se puede eliminar la versión de términos que está actualmente activa.");
        }

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
    public List<AceptacionTerminosResponse> listarAceptaciones() {
        List<AceptacionTerminos> lista = aceptacionRepository.findAllWithUsuarioAndTerminos();
        return lista.stream().map(a -> {
            Long usuarioId = a.getUsuario().getId();
            String correo = a.getUsuario().getCorreo();

            String nombreCompleto = "Usuario #" + usuarioId;
            String numeroDoc = "—";

            Optional<Paciente> pacienteOpt = pacienteRepository.findByUsuarioIdAndActivoTrue(usuarioId);
            if (pacienteOpt.isPresent()) {
                Paciente p = pacienteOpt.get();
                nombreCompleto = String.format("%s %s %s",
                    p.getNombres(),
                    p.getApellidoPaterno(),
                    p.getApellidoMaterno() != null ? p.getApellidoMaterno() : ""
                ).trim();
                numeroDoc = p.getNumeroDocumento();
            } else {
                Optional<Personal> personalOpt = personalRepository.findByUsuarioId(usuarioId);
                if (personalOpt.isPresent()) {
                    Personal pers = personalOpt.get();
                    nombreCompleto = String.format("%s %s %s",
                        pers.getNombres(),
                        pers.getApellidoPaterno(),
                        pers.getApellidoMaterno() != null ? pers.getApellidoMaterno() : ""
                    ).trim();
                    numeroDoc = pers.getNumeroDocumento();
                }
            }

            return new AceptacionTerminosResponse(
                a.getId(),
                usuarioId,
                correo,
                nombreCompleto,
                numeroDoc,
                a.getTerminos().getVersion(),
                a.getTerminos().getTitulo(),
                a.getAceptadoEn(),
                a.getIp(),
                a.getAgenteUsuario()
            );
        }).toList();
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

        if (storageService.existe(CLAVE_S3_DEFECTO)) {
            try {
                return storageService.obtener(CLAVE_S3_DEFECTO);
            } catch (Exception ignored) {}
        }

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

        String urlS3Directa = storageService.obtenerUrl(CLAVE_S3_DEFECTO);
        return new TerminosCondicionesResponse(
            null,
            "Términos y Condiciones de Uso de NeoDent",
            "1.0",
            NOMBRE_ARCHIVO_DEFECTO,
            CLAVE_S3_DEFECTO,
            0L,
            true,
            urlS3Directa,
            urlS3Directa,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
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
        String urlS3Directa = storageService.obtenerUrl(clave);

        return new TerminosCondicionesResponse(
            doc.getId(),
            doc.getTitulo(),
            doc.getVersion(),
            nombreArchivo,
            clave,
            0L,
            Boolean.TRUE.equals(doc.getVigente()),
            urlS3Directa,
            urlS3Directa,
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
