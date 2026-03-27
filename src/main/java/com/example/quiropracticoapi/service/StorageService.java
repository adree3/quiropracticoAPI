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

    /** Almacena bytes en bruto (Miniaturas generadas en RAM) */
    void storeBytes(byte[] content, String path, String contentType);
    
    /** Genera una URL temporal para acceso privado en R2 */
    String generatePresignedUrl(String path);

    /** Genera una URL temporal que fuerza la descarga con un nombre específico */
    String generatePresignedUrl(String path, String downloadFileName);

    /** Verifica si el archivo existe físicamente en el storage */
    boolean exists(String path);

    /** Obtiene el archivo en bruto como array de bytes para actuar como proxy */
    byte[] getFileBytes(String path);

    /** Borra el objeto en R2 mediante su path/key */
    void delete(String path);
}