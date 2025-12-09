package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
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
     * Sirve para ver o descargar el archivo
     * @param filename nombre del archivo
     * @return devuelve el archivo
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}