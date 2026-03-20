package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.config.R2Config;
import com.example.quiropracticoapi.exception.StorageException;
import com.example.quiropracticoapi.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;

@Service
public class R2StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    @Autowired
    public R2StorageService(S3Client s3Client, S3Presigner s3Presigner, R2Config r2Config) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = r2Config.getBucketName();
    }

    @Override
    public void init() {
        // R2 no requiere inicialización de carpetas locales
    }

    /**
     * Almacenamiento legado (para no romper MediaController de golpe). 
     * Sube a una carpeta 'uploads' genérica.
     */
    @Override
    public String store(MultipartFile file) {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String path = "uploads/" + filename;
        store(file, path);
        return filename;
    }

    /**
     * El corazón para el nuevo sistema (Bloque 3).
     * Sube el archivo al path (prefijo) indicado en Cloudflare R2.
     */
    @Override
    public void store(MultipartFile file, String path) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("No se puede subir un archivo vacío.");
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
        } catch (IOException e) {
            throw new StorageException("Fallo al subir archivo a Cloudflare R2: " + path, e);
        }
    }

    /**
     * Firma una URL de descarga para que el frontend pueda ver el archivo privado.
     * TTL: 15 minutos (definido en el plan).
     */
    @Override
    public String generatePresignedUrl(String path) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(builder -> builder.bucket(bucketName).key(path).build())
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public boolean exists(String path) {
        try {
            software.amazon.awssdk.services.s3.model.HeadObjectRequest headRequest = 
                    software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(path)
                        .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void delete(String path) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build();
        s3Client.deleteObject(deleteRequest);
    }
}
