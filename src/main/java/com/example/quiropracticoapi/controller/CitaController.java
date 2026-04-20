package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.CitaDto;
import com.example.quiropracticoapi.dto.CitaRequestDto;
import com.example.quiropracticoapi.dto.HuecoDto;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/citas")
@Tag(name = "Gestión de Citas", description = "Endpoints para reservar, cancelar y ver citas")
public class CitaController {
    private final CitaService citaService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final com.example.quiropracticoapi.service.impl.PdfFirmaService pdfFirmaService;
    private final com.example.quiropracticoapi.service.impl.R2StorageService r2StorageService;

    @Autowired
    public CitaController(CitaService citaService, 
                          org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
                          com.example.quiropracticoapi.service.impl.PdfFirmaService pdfFirmaService,
                          com.example.quiropracticoapi.service.impl.R2StorageService r2StorageService) {
        this.citaService = citaService;
        this.messagingTemplate = messagingTemplate;
        this.pdfFirmaService = pdfFirmaService;
        this.r2StorageService = r2StorageService;
    }

    // Historial de un Cliente
    @Operation(summary = "Ver citas de un cliente", description = "Devuelve el historial completo de citas de un paciente. Paginado.")
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<Page<CitaDto>> getCitasCliente(
            @PathVariable Integer idCliente,
            @RequestParam(value = "estado", required = false) EstadoCita estado,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @PageableDefault(size = 15, sort = "fechaHoraInicio") Pageable pageable) {
        
        log.debug("getCitasCliente - idCliente: {}, fechaInicio: {}, fechaFin: {}", idCliente, fechaInicio, fechaFin);
        
        Page<CitaDto> citas = citaService.getCitasPorCliente(idCliente, estado, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(citas);
    }

    @Operation(summary = "Obtener todas las citas con filtros", description = "Devuelve todas las citas de la clínica con paginación y filtros opcionales de búsqueda, estado y rango de fechas.")
    @GetMapping
    public ResponseEntity<Page<CitaDto>> getAllCitas(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "estado", required = false) EstadoCita estado,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @PageableDefault(size = 15, sort = "fechaHoraInicio") Pageable pageable) {
        Page<CitaDto> citas = citaService.getAllCitas(search, estado, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(citas);
    }

    @Operation(summary = "Obtener KPIs de citas", description = "Devuelve contadores de citas por estado, búsqueda y fecha.")
    @GetMapping("/kpis")
    public ResponseEntity<com.example.quiropracticoapi.dto.CitasKpiDto> getCitasKpis(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "estado", required = false) EstadoCita estado,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        com.example.quiropracticoapi.dto.CitasKpiDto kpis = citaService.getCitasKpis(search, estado, fechaInicio, fechaFin);
        return ResponseEntity.ok(kpis);
    }

    @Operation(summary = "Ver agenda de un día específico", description = "Devuelve todas las citas de la clínica para una fecha dada.")
    @GetMapping("/agenda")
    public ResponseEntity<List<CitaDto>> getAgendaDiaria(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<CitaDto> citas = citaService.getCitasPorFecha(fecha);
        return ResponseEntity.ok(citas);
    }

    @Operation(summary = "Ver agenda por rango de fechas", description = "Devuelve todas las citas entre dos fechas, con filtro opcional por quiropráctico. Útil para vistas semanales/mensuales.")
    @GetMapping("/rango")
    public ResponseEntity<List<CitaDto>> getAgendaPorRango(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "idQuiropractico", required = false) Integer idQuiropractico) {
        List<CitaDto> citas = citaService.getCitasPorRango(desde, hasta, idQuiropractico);
        return ResponseEntity.ok(citas);
    }

    @Operation(summary = "Obtener detalles de una cita", description = "Devuelve la información de una cita específica.")
    @GetMapping("/{idCita}")
    public ResponseEntity<CitaDto> getCitaById(@PathVariable Integer idCita) {
        CitaDto cita = citaService.getCitaById(idCita);
        return ResponseEntity.ok(cita);
    }

    // Crear cita
    @Operation(summary = "Crear una nueva cita", description = "Valida horarios, bloqueos y solapamientos antes de guardar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cita creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Horario no disponible, bloqueo activo o datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente o Quiropráctico no encontrados")
    })
    @PostMapping
    public ResponseEntity<CitaDto> crearCita(@Valid @RequestBody CitaRequestDto request) {
        CitaDto nuevaCita = citaService.crearCita(request);
        return new ResponseEntity<>(nuevaCita, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CitaDto> updateCita(@PathVariable Integer id, @Valid @RequestBody CitaRequestDto request) {
        return ResponseEntity.ok(citaService.updateCita(id, request));
    }

    // Cancelar Cita
    @Operation(summary = "Cancelar una cita", description = "Cambia el estado de la cita a 'cancelada'. No la borra de la base de datos.")
    @PutMapping("/{idCita}/cancelar")
    public ResponseEntity<Void> cancelarCita(@PathVariable Integer idCita) {
        citaService.cancelarCita(idCita);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cambiar estado de la cita", description = "Permite marcar una cita como COMPLETADA o AUSENTE.")
    @PatchMapping("/{idCita}/estado")
    public ResponseEntity<CitaDto> cambiarEstadoCita(
            @PathVariable Integer idCita,
            @RequestParam EstadoCita nuevoEstado) {

        CitaDto citaActualizada = citaService.cambiarEstado(idCita, nuevoEstado);
        return ResponseEntity.ok(citaActualizada);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<HuecoDto>> getHuecos(
            @RequestParam Integer idQuiro,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer idCitaExcluir
    ) {
        return ResponseEntity.ok(citaService.getHuecosDisponibles(idQuiro, fecha, idCitaExcluir));
    }

    // --- MODO KIOSCO / FIRMA ---

    @Operation(summary = "Solicitar firma en Kiosco", description = "Genera un borrador del PDF y envía una señal WebSocket a la tablet con la URL y metadatos.")
    @PostMapping("/{idCita}/solicitar-firma")
    public ResponseEntity<Void> solicitarFirmaKiosco(@PathVariable Integer idCita) {
        CitaDto cita = citaService.getCitaById(idCita);
        
        // 1. Generar el borrador físico en R2 y obtener URL temporal
        String urlBorrador = citaService.generarBorradorFirma(idCita);
        
        // 2. Preparar payload rico para la tablet
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("action", "OPEN_SIGNATURE");
        payload.put("idCita", cita.getIdCita());
        payload.put("nombrePaciente", cita.getNombreClienteCompleto());
        payload.put("urlPdf", urlBorrador);
        
        // Datos de contexto para la UI de la tablet
        payload.put("fecha", cita.getFechaHoraInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        payload.put("horaInicio", cita.getFechaHoraInicio().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        payload.put("horaFin", cita.getFechaHoraFin().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        
        // Info de pago/bono
        if (cita.getInfoPago() != null && cita.getInfoPago().contains("Bono")) {
            payload.put("esBono", true);
            payload.put("infoBono", cita.getInfoPago());
            // Extraer ID de bono si es posible (formato #B-XXX)
            try {
                String[] parts = cita.getInfoPago().split("#B-");
                if (parts.length > 1) {
                    payload.put("idBono", parts[1].split(" ")[0]);
                }
            } catch (Exception e) {}
        } else {
            payload.put("esBono", false);
        }
        
        messagingTemplate.convertAndSend("/topic/kiosk", payload);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{idCita}/firmar")
    public ResponseEntity<java.util.Map<String, Object>> firmarCita(@PathVariable Integer idCita, @RequestBody java.util.Map<String, String> payload) {
        String base64Firma = payload.get("firmaBase64");
        
        // 1. Llamar a CitaService para que procese la firma
        CitaDto citaActualizada = citaService.procesarFirma(idCita, base64Firma);
        
        // Generar URL prefirmada del PDF ya firmado
        String signedUrl = r2StorageService.generatePresignedUrl(citaActualizada.getRutaJustificante());

        // 2. Avisar a kiosco para cerrar pestaña (las que no sean la activa)
        java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
        wsPayload.put("action", "CLOSE_SIGNATURE");
        messagingTemplate.convertAndSend("/topic/kiosk", wsPayload);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("cita", citaActualizada);
        response.put("signedPdfUrl", signedUrl);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener URL del justificante", description = "Devuelve una URL temporal de 15 min para visualizar el justificante en R2.")
    @GetMapping("/{idCita}/justificante")
    public ResponseEntity<java.util.Map<String, String>> getUrlJustificante(@PathVariable Integer idCita) {
        CitaDto cita = citaService.getCitaById(idCita);
        if (cita.getRutaJustificante() == null) {
            return ResponseEntity.notFound().build();
        }
        String url = r2StorageService.generatePresignedUrl(cita.getRutaJustificante());
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }
}
