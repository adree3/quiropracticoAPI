package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final StorageService storageService;

    @Autowired
    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Carga un archivo.
     * @param file nombre del archivo
     * @return el resultado de cargar el archivo
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String filename = storageService.store(file);

        Map<String, String> response = new HashMap<>();
        response.put("url", "/api/media/" + filename);
        response.put("filename", filename);

        return ResponseEntity.ok(response);
    }

    /**
     * Sirve para ver el archivo.
     * En R2, esto devolverá una redirección (302) a la URL temporal del bucket.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Void> getFile(@PathVariable String filename) {
        try {
            String url = storageService.generatePresignedUrl(filename);
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}