package com.neodent.legal.service;

import com.neodent.legal.dto.AceptacionTerminosResponse;
import com.neodent.legal.dto.TerminoPendienteDto;
import com.neodent.legal.dto.TerminosCondicionesResponse;
import com.neodent.legal.dto.TerminosPendientesResponse;
import com.neodent.legal.model.AceptacionDocumentoLegal;
import com.neodent.legal.model.AceptacionTerminos;
import com.neodent.legal.model.DocumentoLegal;
import com.neodent.legal.model.DocumentoLegalVersion;
import com.neodent.legal.model.TerminosCondiciones;
import com.neodent.legal.repository.AceptacionDocumentoLegalRepository;
import com.neodent.legal.repository.AceptacionTerminosRepository;
import com.neodent.legal.repository.DocumentoLegalRepository;
import com.neodent.legal.repository.DocumentoLegalVersionRepository;
import com.neodent.legal.repository.TerminosCondicionesRepository;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.personal.model.Personal;
import com.neodent.personal.repository.PersonalRepository;
import com.neodent.shared.exception.BusinessException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.storage.StorageService;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final DocumentoLegalRepository docLegalRepository;
    private final DocumentoLegalVersionRepository docVersionRepository;
    private final AceptacionDocumentoLegalRepository aceptacionDocRepository;

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final StorageService storageService;

    /* =========================================================
     * CATÁLOGO DE TIPOS DE CONTRATOS / DOCUMENTOS LEGALES
     * ========================================================= */

    @Transactional(readOnly = true)
    public List<DocumentoLegal> listarTipos() {
        List<DocumentoLegal> tipos = docLegalRepository.findByActivoTrueOrderByGrupoAscIdAsc();
        if (tipos.isEmpty()) {
            try {
                tipos = List.of(
                    docLegalRepository.save(new DocumentoLegal("TERMINOS_CONDICIONES", "Términos y Condiciones del Servicio", 1, true)),
                    docLegalRepository.save(new DocumentoLegal("POLITICA_PRIVACIDAD", "Política de Privacidad y Tratamiento de Datos", 1, true))
                );
            } catch (Exception ex) {
                log.warn("No se pudo auto-inicializar catálogo de tipos: {}", ex.getMessage());
            }
        }
        return tipos;
    }

    @Transactional(readOnly = true)
    public List<DocumentoLegal> listarTodosLosTipos() {
        List<DocumentoLegal> tipos = docLegalRepository.findByOrderByGrupoAscIdAsc();
        if (tipos.isEmpty()) {
            return listarTipos();
        }
        return tipos;
    }

    @Transactional
    public DocumentoLegal crearTipo(String codigo, String descripcion, Integer grupo, Boolean activo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El código del documento legal es obligatorio");
        }
        String cod = codigo.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        if (docLegalRepository.existsByCodigo(cod)) {
            throw new BusinessException("Ya existe un tipo de documento con el código: " + cod);
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new BusinessException("La descripción es obligatoria");
        }
        DocumentoLegal tipo = new DocumentoLegal(
            cod,
            descripcion.trim(),
            grupo != null ? grupo : 1,
            activo != null ? activo : true
        );
        return docLegalRepository.save(tipo);
    }

    @Transactional
    public DocumentoLegal editarTipo(Long id, String descripcion, Integer grupo, Boolean activo) {
        DocumentoLegal tipo = docLegalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no encontrado con ID: " + id));
        if (descripcion != null && !descripcion.isBlank()) {
            tipo.setDescripcion(descripcion.trim());
        }
        if (grupo != null) {
            tipo.setGrupo(grupo);
        }
        if (activo != null) {
            tipo.setActivo(activo);
        }
        return docLegalRepository.save(tipo);
    }

    @Transactional
    public void cambiarEstadoTipo(Long id, boolean activo) {
        DocumentoLegal tipo = docLegalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no encontrado con ID: " + id));
        tipo.setActivo(activo);
        docLegalRepository.save(tipo);
    }

    /* =========================================================
     * GESTIÓN DE VERSIONES DE DOCUMENTOS
     * ========================================================= */

    @Transactional(readOnly = true)
    public List<TerminosCondicionesResponse> listarTodos() {
        if (docVersionRepository.count() > 0) {
            return docVersionRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion")).stream()
                .map(this::mapearResponse)
                .toList();
        }
        return repository.findAllByOrderByFechaCreacionDesc().stream()
            .map(this::mapearResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<TerminosCondicionesResponse> listarVersionesPaginado(String buscar, Boolean activo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        if (docVersionRepository.count() > 0) {
            return docVersionRepository.buscarVersionesPaginado(buscar, activo, pageable)
                .map(this::mapearResponse);
        }

        return repository.buscarPaginado(buscar, activo, pageable)
            .map(this::mapearResponse);
    }

    @Transactional
    public TerminosCondicionesResponse subirYCrear(MultipartFile archivo, String titulo, String version, boolean activar) {
        return crear(null, titulo, version, null, null, null, archivo, activar);
    }

    @Transactional
    public TerminosCondicionesResponse crear(
        Long idTipoDocumento,
        String titulo,
        String version,
        String texto,
        String linkTexto,
        String textoAgrupado,
        MultipartFile archivo,
        Boolean activarInmediatamente
    ) {
        validarArchivoPdf(archivo);

        String ver = (version == null || version.isBlank()) ? "1.0" : version.trim();
        String tit = (titulo == null || titulo.isBlank()) ? "Términos y Condiciones del Servicio" : titulo.trim();
        boolean activar = Boolean.TRUE.equals(activarInmediatamente);

        String originalFilename = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : NOMBRE_ARCHIVO_DEFECTO;
        String sanitizedFilename = sanitizarNombre(originalFilename);
        String claveS3 = "documentos/legales/" + sanitizedFilename;

        try {
            byte[] bytes = archivo.getBytes();
            storageService.guardar(claveS3, bytes, CONTENT_TYPE_PDF);
            log.info("Archivo legal guardado en S3 con clave: {}", claveS3);

            // 1. Obtener o crear el tipo de documento legal
            DocumentoLegal docLegal = null;
            if (idTipoDocumento != null) {
                docLegal = docLegalRepository.findById(idTipoDocumento).orElse(null);
            }
            if (docLegal == null) {
                docLegal = docLegalRepository.findByCodigo("TERMINOS_CONDICIONES").orElseGet(() ->
                    docLegalRepository.save(new DocumentoLegal("TERMINOS_CONDICIONES", tit, 1, true))
                );
            }

            if (docVersionRepository.existsByDocumentoLegalIdAndVersion(docLegal.getId(), ver)) {
                throw new BusinessException("Ya existe una versión " + ver + " para " + docLegal.getDescripcion());
            }

            if (activar) {
                // Desactivar versiones activas anteriores de este mismo tipo
                List<DocumentoLegalVersion> previas = docVersionRepository.findByDocumentoLegalIdOrderByFechaCreacionDesc(docLegal.getId());
                for (DocumentoLegalVersion p : previas) {
                    if (Boolean.TRUE.equals(p.getActivo())) {
                        p.setActivo(false);
                        docVersionRepository.save(p);
                    }
                }
                desactivarTodas(tit);
            }

            String txt = (texto != null && !texto.isBlank()) ? texto.trim() : "Acepto los {0} de NeoDent.";
            String lTxt = (linkTexto != null && !linkTexto.isBlank()) ? linkTexto.trim() : (docLegal.getDescripcion());
            String txtAgr = (textoAgrupado != null && !textoAgrupado.isBlank()) ? textoAgrupado.trim() : ("los {0} de NeoDent");

            DocumentoLegalVersion nuevaVersion = new DocumentoLegalVersion(
                docLegal,
                ver,
                claveS3,
                sanitizedFilename,
                txt,
                lTxt,
                txtAgr,
                activar
            );
            DocumentoLegalVersion guardada = docVersionRepository.save(nuevaVersion);

            // Sincronizar en tabla legacy para compatibilidad
            try {
                TerminosCondiciones legacyDoc = new TerminosCondiciones();
                legacyDoc.setTitulo(tit);
                legacyDoc.setVersion(ver);
                legacyDoc.setContenido(claveS3);
                legacyDoc.setFechaPublicacion(LocalDateTime.now());
                legacyDoc.setVigente(activar);
                repository.save(legacyDoc);
            } catch (Exception ex) {
                log.debug("Legacy sync skip: {}", ex.getMessage());
            }

            return mapearResponse(guardada);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Error al leer archivo PDF", ex);
            throw new BusinessException("No se pudo leer el archivo PDF seleccionado: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Error al almacenar documento en S3 con clave {}: {}", claveS3, ex.getMessage(), ex);
            throw new BusinessException("No se pudo guardar el archivo en almacenamiento: " + ex.getMessage());
        }
    }

    @Transactional
    public TerminosCondicionesResponse editar(Long id, String titulo, String version, MultipartFile nuevoArchivo, Boolean activar) {
        // Intentar actualizar en DocumentoLegalVersion si existe
        Optional<DocumentoLegalVersion> optV = docVersionRepository.findById(id);
        if (optV.isPresent()) {
            DocumentoLegalVersion v = optV.get();
            String ver = (version == null || version.isBlank()) ? v.getVersion() : version.trim();
            v.setVersion(ver);

            if (titulo != null && !titulo.isBlank()) {
                v.getDocumentoLegal().setDescripcion(titulo.trim());
                docLegalRepository.save(v.getDocumentoLegal());
            }

            if (nuevoArchivo != null && !nuevoArchivo.isEmpty()) {
                validarArchivoPdf(nuevoArchivo);
                String originalFilename = nuevoArchivo.getOriginalFilename() != null ? nuevoArchivo.getOriginalFilename() : NOMBRE_ARCHIVO_DEFECTO;
                String sanitizedFilename = sanitizarNombre(originalFilename);
                String nuevaClaveS3 = "documentos/legales/" + sanitizedFilename;

                try {
                    byte[] bytes = nuevoArchivo.getBytes();
                    storageService.guardar(nuevaClaveS3, bytes, CONTENT_TYPE_PDF);
                    v.setUrlDocumento(nuevaClaveS3);
                    v.setNombreArchivo(sanitizedFilename);
                } catch (IOException ex) {
                    throw new BusinessException("No se pudo leer el nuevo archivo PDF: " + ex.getMessage());
                }
            }

            if (Boolean.TRUE.equals(activar) && !Boolean.TRUE.equals(v.getActivo())) {
                List<DocumentoLegalVersion> previas = docVersionRepository.findByDocumentoLegalIdOrderByFechaCreacionDesc(v.getDocumentoLegal().getId());
                for (DocumentoLegalVersion p : previas) {
                    p.setActivo(false);
                    docVersionRepository.save(p);
                }
                v.setActivo(true);
            }

            return mapearResponse(docVersionRepository.save(v));
        }

        // Fallback a modelo legacy
        TerminosCondiciones doc = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        String ver = (version == null || version.isBlank()) ? doc.getVersion() : version.trim();
        String tit = (titulo == null || titulo.isBlank()) ? doc.getTitulo() : titulo.trim();
        doc.setTitulo(tit);
        doc.setVersion(ver);

        if (nuevoArchivo != null && !nuevoArchivo.isEmpty()) {
            validarArchivoPdf(nuevoArchivo);
            String originalFilename = nuevoArchivo.getOriginalFilename() != null ? nuevoArchivo.getOriginalFilename() : NOMBRE_ARCHIVO_DEFECTO;
            String sanitizedFilename = sanitizarNombre(originalFilename);
            String nuevaClaveS3 = "documentos/legales/" + sanitizedFilename;

            try {
                byte[] bytes = nuevoArchivo.getBytes();
                storageService.guardar(nuevaClaveS3, bytes, CONTENT_TYPE_PDF);
                doc.setContenido(nuevaClaveS3);
            } catch (IOException ex) {
                throw new BusinessException("No se pudo leer el nuevo archivo PDF: " + ex.getMessage());
            }
        }

        if (Boolean.TRUE.equals(activar) && !Boolean.TRUE.equals(doc.getVigente())) {
            desactivarTodas(tit);
            doc.setVigente(true);
        }

        return mapearResponse(repository.save(doc));
    }

    @Transactional
    public TerminosCondicionesResponse cambiarEstado(Long id, boolean activo) {
        Optional<DocumentoLegalVersion> optV = docVersionRepository.findById(id);
        if (optV.isPresent()) {
            DocumentoLegalVersion v = optV.get();
            if (!activo && Boolean.TRUE.equals(v.getActivo())) {
                throw new BusinessException("No se puede desactivar directamente la versión oficial. Para cambiarla, activa otra versión.");
            }
            if (activo) {
                List<DocumentoLegalVersion> previas = docVersionRepository.findByDocumentoLegalIdOrderByFechaCreacionDesc(v.getDocumentoLegal().getId());
                for (DocumentoLegalVersion p : previas) {
                    p.setActivo(false);
                    docVersionRepository.save(p);
                }
                v.setActivo(true);
            } else {
                v.setActivo(false);
            }
            return mapearResponse(docVersionRepository.save(v));
        }

        TerminosCondiciones doc = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Documento de términos no encontrado"));

        if (!activo && Boolean.TRUE.equals(doc.getVigente())) {
            throw new BusinessException("No se puede desactivar directamente la versión oficial. Para cambiarla, activa otra versión.");
        }

        if (activo) {
            desactivarTodas(doc.getTitulo());
            doc.setVigente(true);
        } else {
            doc.setVigente(false);
        }

        return mapearResponse(repository.save(doc));
    }

    @Transactional
    public void eliminar(Long id) {
        Optional<DocumentoLegalVersion> optV = docVersionRepository.findById(id);
        if (optV.isPresent()) {
            DocumentoLegalVersion v = optV.get();
            if (Boolean.TRUE.equals(v.getActivo())) {
                throw new BusinessException("No se puede eliminar una versión activa.");
            }
            try {
                if (storageService.existe(v.getUrlDocumento())) {
                    storageService.eliminar(v.getUrlDocumento());
                }
            } catch (Exception ex) {
                log.warn("Fallo al eliminar de S3: {}", ex.getMessage());
            }
            docVersionRepository.delete(v);
            return;
        }

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

    /* =========================================================
     * AUDITORÍA Y ACEPTACIONES
     * ========================================================= */

    @Transactional(readOnly = true)
    public List<AceptacionTerminosResponse> listarAceptaciones() {
        if (aceptacionDocRepository.count() > 0) {
            return aceptacionDocRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion")).stream()
                .map(this::mapearAceptacion)
                .toList();
        }
        return aceptacionRepository.findAllWithUsuarioAndTerminos().stream()
            .map(this::mapearAceptacion)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<AceptacionTerminosResponse> listarAceptacionesPaginado(String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        if (aceptacionDocRepository.count() > 0) {
            return aceptacionDocRepository.buscarAceptacionesPaginado(buscar, pageable)
                .map(this::mapearAceptacion);
        }

        return aceptacionRepository.buscarPaginado(buscar, pageable)
            .map(this::mapearAceptacion);
    }

    private AceptacionTerminosResponse mapearAceptacion(AceptacionDocumentoLegal a) {
        Long usuarioId = a.getUsuario().getId();
        String tipoDoc = "DNI";
        String numeroDoc = "-";

        Optional<Paciente> pacienteOpt = pacienteRepository.findByUsuarioIdAndActivoTrue(usuarioId);
        if (pacienteOpt.isPresent()) {
            Paciente p = pacienteOpt.get();
            numeroDoc = p.getNumeroDocumento() != null ? p.getNumeroDocumento() : "-";
            if (p.getTipoDocumento() != null && p.getTipoDocumento().getCodigo() != null) {
                tipoDoc = p.getTipoDocumento().getCodigo();
            }
        } else {
            Optional<Personal> personalOpt = personalRepository.findByUsuarioId(usuarioId);
            if (personalOpt.isPresent()) {
                Personal pers = personalOpt.get();
                numeroDoc = pers.getNumeroDocumento() != null ? pers.getNumeroDocumento() : "-";
                if (pers.getTipoDocumento() != null && pers.getTipoDocumento().getCodigo() != null) {
                    tipoDoc = pers.getTipoDocumento().getCodigo();
                }
            }
        }

        return new AceptacionTerminosResponse(
            a.getId(),
            tipoDoc,
            numeroDoc,
            a.getVersion().getDocumentoLegal().getDescripcion(),
            a.getVersion().getVersion(),
            a.getDireccionIp(),
            a.getAgenteUsuario(),
            a.getFechaCreacion()
        );
    }

    private AceptacionTerminosResponse mapearAceptacion(AceptacionTerminos a) {
        Long usuarioId = a.getUsuario().getId();
        String tipoDoc = "DNI";
        String numeroDoc = "-";

        Optional<Paciente> pacienteOpt = pacienteRepository.findByUsuarioIdAndActivoTrue(usuarioId);
        if (pacienteOpt.isPresent()) {
            Paciente p = pacienteOpt.get();
            numeroDoc = p.getNumeroDocumento() != null ? p.getNumeroDocumento() : "-";
            if (p.getTipoDocumento() != null && p.getTipoDocumento().getCodigo() != null) {
                tipoDoc = p.getTipoDocumento().getCodigo();
            }
        } else {
            Optional<Personal> personalOpt = personalRepository.findByUsuarioId(usuarioId);
            if (personalOpt.isPresent()) {
                Personal pers = personalOpt.get();
                numeroDoc = pers.getNumeroDocumento() != null ? pers.getNumeroDocumento() : "-";
                if (pers.getTipoDocumento() != null && pers.getTipoDocumento().getCodigo() != null) {
                    tipoDoc = pers.getTipoDocumento().getCodigo();
                }
            }
        }

        return new AceptacionTerminosResponse(
            a.getId(),
            tipoDoc,
            numeroDoc,
            a.getTerminos().getTitulo(),
            a.getTerminos().getVersion(),
            a.getIp(),
            a.getAgenteUsuario(),
            a.getAceptadoEn()
        );
    }

    /* =========================================================
     * VISUALIZACIÓN BINARIA (STREAMING PDF)
     * ========================================================= */

    @Transactional(readOnly = true)
    public byte[] obtenerPdfPorId(Long id) {
        Optional<DocumentoLegalVersion> optV = docVersionRepository.findById(id);
        if (optV.isPresent()) {
            String clave = optV.get().getUrlDocumento();
            if (storageService.existe(clave)) {
                return storageService.obtener(clave);
            }
        }

        TerminosCondiciones doc = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Documento de términos no encontrado"));

        return resolverBytesDocumento(doc);
    }

    @Transactional(readOnly = true)
    public byte[] obtenerPdfActivo() {
        var optV = docVersionRepository.findFirstByDocumentoLegalCodigoAndActivoTrueOrderByFechaCreacionDesc("TERMINOS_CONDICIONES");
        if (optV.isEmpty()) {
            optV = docVersionRepository.findByActivoTrueOrderByFechaCreacionDesc().stream().findFirst();
        }
        if (optV.isPresent()) {
            String clave = optV.get().getUrlDocumento();
            if (storageService.existe(clave)) {
                return storageService.obtener(clave);
            }
        }

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
    public byte[] obtenerPdfPorNombreArchivo(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResourceNotFoundException("Nombre de archivo no especificado");
        }

        // 1. Buscar en documento_legal_version por nombre_archivo o url_documento
        Optional<DocumentoLegalVersion> optV = docVersionRepository.findByNombreArchivo(filename);
        if (optV.isPresent()) {
            String clave = optV.get().getUrlDocumento();
            if (storageService.existe(clave)) {
                return storageService.obtener(clave);
            }
        }

        List<DocumentoLegalVersion> todasVersiones = docVersionRepository.findAll();
        for (DocumentoLegalVersion v : todasVersiones) {
            if (v.getNombreArchivo().equalsIgnoreCase(filename)
                || v.getUrlDocumento().equalsIgnoreCase(filename)
                || v.getUrlDocumento().endsWith("/" + filename)) {
                if (storageService.existe(v.getUrlDocumento())) {
                    return storageService.obtener(v.getUrlDocumento());
                }
            }
        }

        // 2. Buscar en terminos_condiciones (legacy)
        List<TerminosCondiciones> todosLegacy = repository.findAll();
        Optional<TerminosCondiciones> coincidencia = todosLegacy.stream()
            .filter(doc -> {
                if (doc.getContenido() == null) return false;
                String contenido = doc.getContenido();
                String nombreReal = contenido.contains("/")
                    ? contenido.substring(contenido.lastIndexOf('/') + 1)
                    : contenido;
                return nombreReal.equalsIgnoreCase(filename)
                    || contenido.equalsIgnoreCase(filename)
                    || contenido.endsWith("/" + filename);
            })
            .findFirst();

        if (coincidencia.isPresent()) {
            return resolverBytesDocumento(coincidencia.get());
        }

        // 3. Buscar directo en la carpeta S3
        String claveDirecta = "documentos/legales/" + filename;
        if (storageService.existe(claveDirecta)) {
            try {
                return storageService.obtener(claveDirecta);
            } catch (Exception ex) {
                log.warn("Fallo al obtener archivo directo de S3: {}", ex.getMessage());
            }
        }

        throw new ResourceNotFoundException("No se encontró el documento legal con nombre: " + filename);
    }

    @Transactional(readOnly = true)
    public String obtenerNombreArchivoDescargaActivo() {
        var optV = docVersionRepository.findFirstByDocumentoLegalCodigoAndActivoTrueOrderByFechaCreacionDesc("TERMINOS_CONDICIONES");
        if (optV.isPresent()) {
            return optV.get().getNombreArchivo();
        }
        return repository.findFirstByVigenteTrueOrderByFechaPublicacionDesc()
            .map(doc -> generarNombreLimpio(doc.getTitulo()))
            .orElse(NOMBRE_ARCHIVO_DEFECTO);
    }

    @Transactional(readOnly = true)
    public String obtenerNombreArchivoDescargaPorId(Long id) {
        Optional<DocumentoLegalVersion> optV = docVersionRepository.findById(id);
        if (optV.isPresent()) {
            return optV.get().getNombreArchivo();
        }
        return repository.findById(id)
            .map(doc -> generarNombreLimpio(doc.getTitulo()))
            .orElse("terminos_y_condiciones.pdf");
    }

    public static String generarNombreLimpio(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return NOMBRE_ARCHIVO_DEFECTO;
        }
        String normalizado = java.text.Normalizer.normalize(titulo.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        return normalizado.isEmpty() ? NOMBRE_ARCHIVO_DEFECTO : (normalizado + ".pdf");
    }

    @Transactional(readOnly = true)
    public TerminosCondicionesResponse obtenerInfoActivo() {
        var optV = docVersionRepository.findFirstByDocumentoLegalCodigoAndActivoTrueOrderByFechaCreacionDesc("TERMINOS_CONDICIONES");
        if (optV.isEmpty()) {
            optV = docVersionRepository.findByActivoTrueOrderByFechaCreacionDesc().stream().findFirst();
        }

        if (optV.isPresent()) {
            return mapearResponse(optV.get());
        }

        var opt = repository.findFirstByVigenteTrueOrderByFechaPublicacionDesc();
        if (opt.isPresent()) {
            return mapearResponse(opt.get());
        }

        String urlS3Directa = storageService.obtenerUrl(CLAVE_S3_DEFECTO);
        return new TerminosCondicionesResponse(
            null,
            "TERMINOS_CONDICIONES",
            "Términos y Condiciones del Servicio",
            "1.0",
            NOMBRE_ARCHIVO_DEFECTO,
            CLAVE_S3_DEFECTO,
            "Acepto los {0} de NeoDent.",
            "Términos y condiciones",
            "los {0} de NeoDent",
            0L,
            true,
            "/terminos/" + NOMBRE_ARCHIVO_DEFECTO,
            urlS3Directa,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    /* =========================================================
     * VERIFICACIÓN Y REGISTRO DE ACEPTACIÓN POR PACIENTE / USUARIO
     * ========================================================= */

    @Transactional(readOnly = true)
    public TerminosPendientesResponse verificarPendiente(Long usuarioId) {
        List<DocumentoLegalVersion> activas = docVersionRepository.findByActivoTrueOrderByFechaCreacionDesc();

        if (!activas.isEmpty()) {
            List<TerminoPendienteDto> pendientes = new java.util.ArrayList<>();
            java.util.Set<Long> tiposVistos = new java.util.HashSet<>();

            for (DocumentoLegalVersion v : activas) {
                Long tipoId = v.getDocumentoLegal().getId();
                if (tiposVistos.add(tipoId)) {
                    boolean yaAcepto = aceptacionDocRepository.existsByUsuarioIdAndVersionId(usuarioId, v.getId());
                    if (!yaAcepto) {
                        pendientes.add(new TerminoPendienteDto(
                            v.getId(),
                            v.getDocumentoLegal().getCodigo(),
                            v.getDocumentoLegal().getDescripcion(),
                            v.getVersion(),
                            v.getNombreArchivo(),
                            v.getTexto(),
                            v.getLinkTexto(),
                            v.getTextoAgrupado(),
                            "/terminos/" + v.getNombreArchivo()
                        ));
                    }
                }
            }

            return new TerminosPendientesResponse(!pendientes.isEmpty(), pendientes);
        }

        // Fallback a modelo legacy
        List<TerminosCondiciones> legacyActivos = repository.findByVigenteTrueOrderByFechaPublicacionDesc();
        if (legacyActivos.isEmpty()) {
            return new TerminosPendientesResponse(false, List.of());
        }

        List<TerminoPendienteDto> pendientes = new java.util.ArrayList<>();
        java.util.Set<String> titulosVistos = new java.util.HashSet<>();

        for (TerminosCondiciones doc : legacyActivos) {
            String claveTipo = doc.getTitulo() != null ? doc.getTitulo().trim().toLowerCase() : "default";
            if (titulosVistos.add(claveTipo)) {
                boolean yaAcepto = aceptacionRepository.existsByUsuarioIdAndTerminosId(usuarioId, doc.getId());
                if (!yaAcepto) {
                    String clave = doc.getContenido() != null ? doc.getContenido() : "";
                    String nombreArchivo = clave.contains("/")
                        ? clave.substring(clave.lastIndexOf('/') + 1)
                        : (clave.isBlank() ? NOMBRE_ARCHIVO_DEFECTO : clave);

                    pendientes.add(new TerminoPendienteDto(
                        doc.getId(),
                        "TERMINOS_CONDICIONES",
                        doc.getTitulo(),
                        doc.getVersion(),
                        nombreArchivo,
                        "Acepto los {0} de NeoDent.",
                        "Términos y condiciones",
                        "los {0} de NeoDent",
                        "/terminos/" + nombreArchivo
                    ));
                }
            }
        }

        return new TerminosPendientesResponse(!pendientes.isEmpty(), pendientes);
    }

    @Transactional
    public void registrarAceptacionUsuario(Long usuarioId, Long terminosId, HttpServletRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        String ip = null;
        String userAgent = null;
        if (request != null) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            } else if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 500);
            }
        }

        // 1. Guardar en aceptacion_documento_legal si el ID corresponde a una DocumentoLegalVersion
        if (terminosId != null) {
            Optional<DocumentoLegalVersion> optV = docVersionRepository.findById(terminosId);
            if (optV.isPresent()) {
                DocumentoLegalVersion v = optV.get();
                if (!aceptacionDocRepository.existsByUsuarioIdAndVersionId(usuarioId, v.getId())) {
                    AceptacionDocumentoLegal adl = new AceptacionDocumentoLegal(
                        v,
                        usuario,
                        true,
                        "Web",
                        ip,
                        userAgent
                    );
                    aceptacionDocRepository.save(adl);
                    log.info("Usuario {} aceptó versión {} de documento legal {}", usuarioId, v.getVersion(), v.getDocumentoLegal().getDescripcion());
                }
                return;
            }
        }

        // Si terminosId no se envió o no es de la nueva tabla, buscar por defecto o guardar todas las activas
        List<DocumentoLegalVersion> activas = docVersionRepository.findByActivoTrueOrderByFechaCreacionDesc();
        if (!activas.isEmpty()) {
            for (DocumentoLegalVersion v : activas) {
                if (!aceptacionDocRepository.existsByUsuarioIdAndVersionId(usuarioId, v.getId())) {
                    AceptacionDocumentoLegal adl = new AceptacionDocumentoLegal(
                        v,
                        usuario,
                        true,
                        "Web",
                        ip,
                        userAgent
                    );
                    aceptacionDocRepository.save(adl);
                }
            }
            return;
        }

        // Fallback a modelo legacy
        TerminosCondiciones terminos;
        if (terminosId != null) {
            terminos = repository.findById(terminosId)
                .orElseThrow(() -> new ResourceNotFoundException("Versión de términos no encontrada con ID: " + terminosId));
        } else {
            terminos = repository.findFirstByVigenteTrueOrderByFechaPublicacionDesc()
                .orElseThrow(() -> new BusinessException("No hay una versión de términos activa para aceptar"));
        }

        if (aceptacionRepository.existsByUsuarioIdAndTerminosId(usuarioId, terminos.getId())) {
            return;
        }

        AceptacionTerminos aceptacion = new AceptacionTerminos();
        aceptacion.setUsuario(usuario);
        aceptacion.setTerminos(terminos);
        aceptacion.setAceptadoEn(LocalDateTime.now());
        aceptacion.setIp(ip);
        aceptacion.setAgenteUsuario(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent);
        aceptacionRepository.save(aceptacion);
    }

    private void desactivarTodas(String titulo) {
        try {
            List<TerminosCondiciones> todas = repository.findAll();
            for (TerminosCondiciones t : todas) {
                if (Boolean.TRUE.equals(t.getVigente())) {
                    if (titulo == null || t.getTitulo() == null || t.getTitulo().trim().equalsIgnoreCase(titulo.trim())) {
                        t.setVigente(false);
                    }
                }
            }
            repository.saveAll(todas);
        } catch (Exception ex) {
            log.warn("Fallo al desactivar versiones anteriores del mismo tipo: {}", ex.getMessage());
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

    public TerminosCondicionesResponse mapearResponse(DocumentoLegalVersion v) {
        String clave = v.getUrlDocumento();
        String nombreArchivo = v.getNombreArchivo();
        String urlS3Directa = storageService.obtenerUrl(clave);
        String urlVisualizar = "/terminos/" + nombreArchivo;

        return new TerminosCondicionesResponse(
            v.getId(),
            v.getDocumentoLegal().getCodigo(),
            v.getDocumentoLegal().getDescripcion(),
            v.getVersion(),
            nombreArchivo,
            clave,
            v.getTexto(),
            v.getLinkTexto(),
            v.getTextoAgrupado(),
            0L,
            v.getActivo(),
            urlVisualizar,
            urlS3Directa,
            v.getFechaCreacion(),
            v.getFechaCreacion()
        );
    }

    private TerminosCondicionesResponse mapearResponse(TerminosCondiciones doc) {
        String clave = doc.getContenido() != null ? doc.getContenido() : CLAVE_S3_DEFECTO;
        String nombreArchivo = clave.contains("/") ? clave.substring(clave.lastIndexOf('/') + 1) : NOMBRE_ARCHIVO_DEFECTO;
        String urlS3Directa = storageService.obtenerUrl(clave);
        String urlVisualizar = "/terminos/" + nombreArchivo;

        return new TerminosCondicionesResponse(
            doc.getId(),
            "TERMINOS_CONDICIONES",
            doc.getTitulo(),
            doc.getVersion(),
            nombreArchivo,
            clave,
            "Acepto los {0} de NeoDent.",
            "Términos y condiciones",
            "los {0} de NeoDent",
            0L,
            Boolean.TRUE.equals(doc.getVigente()),
            urlVisualizar,
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
        if (nombre == null || nombre.isBlank()) {
            return NOMBRE_ARCHIVO_DEFECTO;
        }
        String simple = nombre.contains("\\") ? nombre.substring(nombre.lastIndexOf('\\') + 1) : nombre;
        simple = simple.contains("/") ? simple.substring(simple.lastIndexOf('/') + 1) : simple;

        String base = simple;
        if (simple.toLowerCase().endsWith(".pdf")) {
            base = simple.substring(0, simple.length() - 4);
        }

        String normalizado = Normalizer.normalize(base.trim().toLowerCase(), Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .replaceAll("\\s+", "_")
            .replaceAll("[^a-z0-9._-]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+|_+$", "");

        if (normalizado.isEmpty()) {
            normalizado = "documento";
        }
        return normalizado + ".pdf";
    }
}
