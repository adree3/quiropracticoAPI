package com.example.quiropracticoapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class R2Config {

    @Value("${r2.endpoint}")
    private String endpoint;

    @Value("${r2.access-key}")
    private String accessKey;

    @Value("${r2.secret-key}")
    private String secretKey;

    @Value("${r2.bucket-name}")
    private String bucketName;

    /**
     * Cliente S3 configurado para apuntar al endpoint de Cloudflare R2.
     * R2 es 100% compatible con la API de S3, por lo que usamos la misma librería
     * simplemente sobreescribiendo el endpoint y la región (r2 usa 'auto').
     */
    @Bean
    public S3Client s3Client() {
        String cleanEndpoint = endpoint != null ? endpoint.trim() : "";
        String cleanAccessKey = accessKey != null ? accessKey.trim() : "";
        String cleanSecretKey = secretKey != null ? secretKey.trim() : "";

        return S3Client.builder()
                .endpointOverride(URI.create(cleanEndpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cleanAccessKey, cleanSecretKey)
                ))
                .build();
    }

    /**
     * Presigner separado del S3Client para generar Presigned URLs.
     * Se necesita una instancia distinta porque el proceso de firma es HTTP GET,
     * no una operación de subida.
     */
    @Bean
    public S3Presigner s3Presigner() {
        String cleanEndpoint = endpoint != null ? endpoint.trim() : "";
        String cleanAccessKey = accessKey != null ? accessKey.trim() : "";
        String cleanSecretKey = secretKey != null ? secretKey.trim() : "";

        return S3Presigner.builder()
                .endpointOverride(URI.create(cleanEndpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cleanAccessKey, cleanSecretKey)
                ))
                .build();
    }

    public String getBucketName() {
        return bucketName != null ? bucketName.trim() : "";
    }
}
