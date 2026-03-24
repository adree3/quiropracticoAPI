package com.example.quiropracticoapi.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {
    void init();
    
    /** Almacenamiento genérico (R2) */
    String store(MultipartFile file);
    
    /** Almacenamiento en R2 con path controlado (Saga) */
    void store(MultipartFile file, String path);
    
    /** Genera una URL temporal para acceso privado en R2 */
    String generatePresignedUrl(String path);

    /** Verifica si el archivo existe físicamente en el storage */
    boolean exists(String path);

    /** Obtiene el archivo en bruto como array de bytes para actuar como proxy */
    byte[] getFileBytes(String path);

    /** Borra el objeto en R2 mediante su path/key */
    void delete(String path);
}