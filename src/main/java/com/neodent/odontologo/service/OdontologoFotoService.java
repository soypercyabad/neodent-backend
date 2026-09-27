package com.neodent.odontologo.service;

import com.neodent.odontologo.model.Odontologo;
import com.neodent.odontologo.repository.OdontologoRepository;
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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdontologoFotoService {

    static {
        ImageIO.scanForPlugins();
    }

    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_LADO_PERFIL = 1200;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final String CONTENT_TYPE_WEBP = "image/webp";

    private final OdontologoRepository odontologoRepository;
    private final PersonalRepository personalRepository;
    private final StorageService storageService;



    @Transactional
    public Odontologo subirPorUsuario(Long usuarioId, MultipartFile archivo) {
        validarArchivo(archivo);

        Odontologo odontologo = buscarPorUsuario(usuarioId);
        byte[] contenidoWebp = convertirAWebp(archivo);

        String identificador = UUID.randomUUID().toString().replace("-", "");
        String nombreNuevo = "perfil-" + identificador + ".webp";
        String claveNueva = construirClave(odontologo.getId(), nombreNuevo);
        String nombreAnterior = odontologo.getFotoNombreArchivo();

        storageService.guardar(claveNueva, contenidoWebp, CONTENT_TYPE_WEBP);

        try {
            odontologo.setFotoNombreArchivo(nombreNuevo);
            odontologoRepository.saveAndFlush(odontologo);
        } catch (RuntimeException ex) {
            try {
                storageService.eliminar(claveNueva);
            } catch (Exception ignored) {
            }
            throw ex;
        }

        if (nombreAnterior != null && !nombreAnterior.isBlank() && !nombreAnterior.equals(nombreNuevo)) {
            String claveAnterior = construirClave(odontologo.getId(), nombreAnterior);
            try {
                storageService.eliminar(claveAnterior);
            } catch (Exception ignored) {
            }
        }

        return odontologo;
    }



    @Transactional
    public Odontologo eliminarPorUsuario(Long usuarioId) {
        Odontologo odontologo = buscarPorUsuario(usuarioId);
        String nombreAnterior = odontologo.getFotoNombreArchivo();

        if (nombreAnterior == null || nombreAnterior.isBlank()) {
            return odontologo;
        }

        String claveAnterior = construirClave(odontologo.getId(), nombreAnterior);

        odontologo.setFotoNombreArchivo(null);
        odontologoRepository.saveAndFlush(odontologo);

        try {
            storageService.eliminar(claveAnterior);
        } catch (Exception ignored) {
        }

        return odontologo;
    }



    @Transactional(readOnly = true)
    public FotoOdontologo obtener(Long odontologoId) {
        Odontologo odontologo = odontologoRepository.findById(odontologoId)
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));

        String nombreArchivo = odontologo.getFotoNombreArchivo();
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            throw new ResourceNotFoundException("El odontólogo no tiene fotografía registrada");
        }

        String clave = construirClave(odontologo.getId(), nombreArchivo);
        byte[] contenido;

        try {
            contenido = storageService.obtener(clave);
        } catch (Exception ex) {
            throw new ResourceNotFoundException("No se pudo obtener la fotografía del odontólogo");
        }

        return new FotoOdontologo(contenido, CONTENT_TYPE_WEBP, nombreArchivo);
    }



    private Odontologo buscarPorUsuario(Long usuarioId) {
        Personal personal = personalRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado"));

        return odontologoRepository.findByPersonalId(personal.getId())
                .orElseThrow(() -> new BusinessException("La fotografía solo puede asignarse a un odontólogo."));
    }



    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Selecciona una fotografía.");
        }

        if (archivo.getSize() > MAX_BYTES) {
            throw new BusinessException("La fotografía no puede superar los 5 MB.");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new BusinessException("La fotografía debe ser JPG, PNG o WEBP.");
        }
    }



    private byte[] convertirAWebp(MultipartFile archivo) {
        BufferedImage imagenOriginal = leerImagen(archivo);
        BufferedImage imagenPerfil = normalizarYRedimensionar(imagenOriginal);
        return escribirWebp(imagenPerfil);
    }



    private BufferedImage leerImagen(MultipartFile archivo) {
        ImageIO.scanForPlugins();

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo de la fotografía.");
        }

        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("El archivo seleccionado está vacío.");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {

            if (iis != null) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                while (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(iis, true, true);
                        BufferedImage imagen = reader.read(0);
                        if (imagen != null) {
                            return imagen;
                        }
                    } catch (Exception ex) {
                        log.warn("Fallo al decodificar imagen con reader {}: {}", reader.getClass().getSimpleName(), ex.getMessage());
                    } finally {
                        reader.dispose();
                    }
                }
            }

            try (ByteArrayInputStream fallbackStream = new ByteArrayInputStream(bytes)) {
                BufferedImage fallbackImg = ImageIO.read(fallbackStream);
                if (fallbackImg != null) {
                    return fallbackImg;
                }
            }

            validarFormatoNoCompatible(bytes);

            log.warn("No se pudo decodificar la imagen. ContentType: {}, Size: {} bytes, Formatos disponibles: {}",
                    archivo.getContentType(), bytes.length, Arrays.toString(ImageIO.getReaderFormatNames()));
            throw new BusinessException("El archivo seleccionado no contiene una imagen válida.");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al procesar la imagen: {}", ex.getMessage(), ex);
            throw new BusinessException("No se pudo procesar la fotografía seleccionada.");
        }
    }



    private void validarFormatoNoCompatible(byte[] bytes) {
        if (bytes.length < 16) {
            return;
        }

        String encabezado = new String(bytes, 0, Math.min(bytes.length, 64)).toLowerCase();

        if (encabezado.contains("ftypavif")) {
            throw new BusinessException("El archivo seleccionado es una imagen en formato AVIF (con extensión modificada). El sistema admite JPG, PNG o WEBP.");
        }

        if (encabezado.contains("ftypheic") || encabezado.contains("ftypmif1")) {
            throw new BusinessException("El archivo seleccionado es una imagen en formato HEIC de Apple (con extensión modificada). El sistema admite JPG, PNG o WEBP.");
        }

        if (encabezado.contains("<!doctype") || encabezado.contains("<html")) {
            throw new BusinessException("El archivo seleccionado es un documento HTML y no una imagen real.");
        }

        if (encabezado.contains("<svg") || encabezado.contains("<?xml")) {
            throw new BusinessException("El formato vectorial SVG no está soportado. Selecciona una fotografía JPG, PNG o WEBP.");
        }
    }



    private BufferedImage normalizarYRedimensionar(BufferedImage original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        double escala = Math.min(1d, Math.min((double) MAX_LADO_PERFIL / ancho, (double) MAX_LADO_PERFIL / alto));
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
        int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));

        BufferedImage normalizada = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = normalizada.createGraphics();

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, nuevoAncho, nuevoAlto);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
        } finally {
            graphics.dispose();
        }

        return normalizada;
    }



    private byte[] escribirWebp(BufferedImage imagen) {
        ImageIO.scanForPlugins();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");

        if (!writers.hasNext()) {
            writers = ImageIO.getImageWritersByMIMEType(CONTENT_TYPE_WEBP);
        }

        if (!writers.hasNext()) {
            throw new IllegalStateException("No se encontró un encoder WebP. Verifica la dependencia webp-imageio.");
        }

        ImageWriter writer = writers.next();

        try (
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageOutputStream imageOutput = ImageIO.createImageOutputStream(salida)
        ) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(imagen, null, null), writer.getDefaultWriteParam());
            imageOutput.flush();

            byte[] resultado = salida.toByteArray();

            if (resultado.length == 0) {
                throw new IllegalStateException("La conversión a WebP produjo un archivo vacío.");
            }

            return resultado;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo convertir la fotografía a WebP.", ex);
        } finally {
            writer.dispose();
        }
    }



    public String construirClave(Long odontologoId, String nombreArchivo) {
        return "odontologos/" + odontologoId + "/perfil/" + nombreArchivo;
    }



    public record FotoOdontologo(
            byte[] contenido,
            String contentType,
            String nombreArchivo
    ) {
    }
}