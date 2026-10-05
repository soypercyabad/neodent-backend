package com.neodent.storage;

public interface StorageService {

    void guardar(
        String clave,
        byte[] contenido,
        String contentType
    );

    byte[] obtener(String clave);

    void eliminar(String clave);

    boolean existe(String clave);

    String obtenerUrl(String clave);
}
