package com.example.quiropracticoapi.service.impl;


import com.example.quiropracticoapi.dto.ClienteDetalleProjection;
import com.example.quiropracticoapi.dto.ClienteDto;
import com.example.quiropracticoapi.dto.ClienteRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.mapper.ClienteMapper;
import com.example.quiropracticoapi.model.BonoActivo;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.GrupoFamiliar;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.BonoActivoRepository;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.repository.GrupoFamiliarRepository;
import com.example.quiropracticoapi.service.ClienteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.example.quiropracticoapi.model.DocumentoCliente;
import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import com.example.quiropracticoapi.repository.DocumentoClienteRepository;
import com.example.quiropracticoapi.service.impl.PdfFirmaService;
import com.example.quiropracticoapi.service.impl.R2StorageService;
import com.example.quiropracticoapi.util.StoragePathBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ClienteServiceImpl implements ClienteService {
    private static final Logger log = LoggerFactory.getLogger(ClienteServiceImpl.class);
    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final GrupoFamiliarRepository grupoFamiliarRepository;
    private final CitaRepository citaRepository;
    private final BonoActivoRepository bonoActivoRepository;
    private final AuditoriaServiceImpl auditoriaServiceImpl;
    private final BonoServiceImpl bonoServiceImpl;
    private final com.example.quiropracticoapi.repository.PagoRepository pagoRepository;
    private final com.example.quiropracticoapi.mapper.CitaMapper citaMapper;
    private final PdfFirmaService pdfFirmaService;
    private final R2StorageService r2StorageService;
    private final StoragePathBuilder storagePathBuilder;
    private final DocumentoClienteRepository documentoClienteRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            ClienteMapper clienteMapper,
            GrupoFamiliarRepository grupoFamiliarRepository,
            CitaRepository citaRepository,
            BonoActivoRepository bonoActivoRepository, 
            AuditoriaServiceImpl auditoriaServiceImpl, 
            BonoServiceImpl bonoServiceImpl,
            com.example.quiropracticoapi.repository.PagoRepository pagoRepository,
            com.example.quiropracticoapi.mapper.CitaMapper citaMapper,
            PdfFirmaService pdfFirmaService,
            R2StorageService r2StorageService,
            StoragePathBuilder storagePathBuilder,
            DocumentoClienteRepository documentoClienteRepository,
            SimpMessagingTemplate messagingTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.grupoFamiliarRepository = grupoFamiliarRepository;
        this.citaRepository = citaRepository;
        this.bonoActivoRepository = bonoActivoRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
        this.bonoServiceImpl = bonoServiceImpl;
        this.pagoRepository = pagoRepository;
        this.citaMapper = citaMapper;
        this.pdfFirmaService = pdfFirmaService;
        this.r2StorageService = r2StorageService;
        this.storagePathBuilder = storagePathBuilder;
        this.documentoClienteRepository = documentoClienteRepository;
        this.messagingTemplate = messagingTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }


    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<ClienteDto> getAllClientes(Boolean activo, Pageable pageable) {
        Page<Cliente> paginaClientes;
        if (activo == null) {
            paginaClientes = clienteRepository.findAll(pageable);
        } else {
            paginaClientes = clienteRepository.findByActivo(activo, pageable);
        }
        return paginaClientes.map(clienteMapper::toClienteDto);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ClienteDto getClienteById(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro el cliente con id: " + id));
        return enrichClienteDetalleDto(cliente);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ClienteDto createCliente(ClienteRequestDto clienteRequestDto) {
        clienteRepository.findByEmail(clienteRequestDto.getEmail()).ifPresent(c -> {
            throw new IllegalArgumentException("El email" + clienteRequestDto.getEmail() + " ya existe.");
        });
        Cliente cliente = clienteMapper.toCliente(clienteRequestDto);
        cliente.setActivo(true);
        
        Long tenantId = com.example.quiropracticoapi.config.TenantContext.getTenantId();
        com.example.quiropracticoapi.model.Clinica clinica = new com.example.quiropracticoapi.model.Clinica();
        if (tenantId == null || tenantId == 0L) {
            // Intentar obtener la clínica del usuario autenticado o usar 1L por defecto
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.example.quiropracticoapi.model.Usuario) {
                com.example.quiropracticoapi.model.Usuario u = (com.example.quiropracticoapi.model.Usuario) auth.getPrincipal();
                if (u.getClinica() != null) {
                    clinica.setIdClinica(u.getClinica().getIdClinica());
                } else {
                    clinica.setIdClinica(1L);
                }
            } else {
                clinica.setIdClinica(1L);
            }
        } else {
            clinica.setIdClinica(tenantId);
        }
        cliente.setClinica(clinica);

        Cliente clienteGuardado = clienteRepository.save(cliente);
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR,
                "CLIENTE",
                clienteGuardado.getIdCliente().toString(),
                "Nuevo paciente registrado: " + clienteGuardado.getNombre() + " " + clienteGuardado.getApellidos() +
                        "."
        );
        return clienteMapper.toClienteDto(clienteGuardado);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ClienteDto updateCliente(Integer id, ClienteRequestDto clienteRequestDto) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        String datosPrevios = clienteExistente.getNombre() + " " + clienteExistente.getApellidos();
        clienteMapper.updateClienteFromDto(clienteRequestDto, clienteExistente);
        Cliente clienteActualizado = clienteRepository.save(clienteExistente);
        
        String detalle = "Actualización de datos personales. Nombre: " + clienteActualizado.getNombre() + " " + clienteActualizado.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "CLIENTE",
                id.toString(),
                detalle
        );

        return clienteMapper.toClienteDto(clienteActualizado);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteCliente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        String detalle = "Paciente desactivado (enviado a papelera): " + cliente.getNombre() + " " + cliente.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.ELIMINAR_LOGICO,
                "CLIENTE",
                id.toString(),
                detalle
        );
    }



    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<ClienteDto> findClientesWithFilters(
        Boolean activo,
        String texto,
        Integer lastActivityDays,
        Pageable pageable
    ) {
        // Para filtros temporales complejos (lastActivityDays) seguimos usando el filtrado por entidad
        LocalDateTime ultimaCitaDesde = null;
        if (lastActivityDays != null) {
            ultimaCitaDesde = LocalDateTime.now().minusDays(lastActivityDays);
        }

        Long tenantId = com.example.quiropracticoapi.config.TenantContext.getTenantId();

        log.info("[DEBUG-TENANT] Servicio: ClienteServiceImpl | TenantContext: {} | Texto: {}", tenantId, texto);
        // Búsqueda normal optimizada (Sin N+1)
        Page<ClienteDetalleProjection> projectionPage = clienteRepository.findClientesOptimized(
            activo, texto, tenantId, ultimaCitaDesde, pageable
        );

        return projectionPage.map(p -> {
            ClienteDto dto = new ClienteDto();
            dto.setIdCliente(p.getIdCliente());
            dto.setNombre(p.getNombre());
            dto.setApellidos(p.getApellidos());
            dto.setEmail(p.getEmail());
            dto.setTelefono(p.getTelefono());
            dto.setActivo(p.getActivo());
            dto.setLopdFirmada(p.getLopdFirmada() != null && p.getLopdFirmada());
            dto.setFechaAlta(p.getFechaCreacion());
            
            // Datos agregados ya calculados en SQL
            dto.setCitasPendientes(p.getCountCitasPendientes());
            dto.setBonosActivos(p.getCountBonosActivos());
            dto.setTieneFamiliares(p.getTieneFamiliares() != null && p.getTieneFamiliares() > 0);
            dto.setUltimaCita(p.getUltimaCita());
            
            return dto;
        });
    }

    private ClienteDto enrichClienteDetalleDto(Cliente cliente) {
        ClienteDto dto = clienteMapper.toClienteDto(cliente);
        Integer idCliente = cliente.getIdCliente();
        
        // Citas métricas
        long completadas = citaRepository.countByClienteIdClienteAndEstado(idCliente, EstadoCita.completada);
        long canceladas = citaRepository.countByClienteIdClienteAndEstado(idCliente, EstadoCita.cancelada);
        long ausentes = citaRepository.countByClienteIdClienteAndEstado(idCliente, EstadoCita.ausente);
        long programadas = citaRepository.countByClienteIdClienteAndEstado(idCliente, EstadoCita.programada);
        
        dto.setCitasPendientes((int) programadas);
        dto.setCitasCompletadas(completadas);
        dto.setCitasCanceladas(canceladas);
        dto.setCitasAusentes(ausentes);
        
        // Pagos métricas
        dto.setDeudaPendiente(pagoRepository.sumDeudaPendienteByClienteId(idCliente));
        dto.setPagosPendientesCount(pagoRepository.countByClienteIdClienteAndPagadoFalse(idCliente));
        
        // Bonos activos (Conteo escalar)
        long bonosActivosCount = bonoActivoRepository.countByClienteIdClienteAndSesionesRestantesGreaterThan(idCliente, 0);
        dto.setBonosActivos((int) bonosActivosCount);
        
        // Familiares (Exists escalar)
        boolean tieneFamiliares = grupoFamiliarRepository.existsByPropietarioIdCliente(idCliente);
        dto.setTieneFamiliares(tieneFamiliares);
        
        // Última cita (Scalar Max Date)
        LocalDateTime ultimaCita = citaRepository.findUltimaCitaCompletadaFecha(idCliente);
        dto.setUltimaCita(ultimaCita);
        
        citaRepository.findFirstByClienteIdClienteAndEstadoAndFechaHoraInicioGreaterThanEqualOrderByFechaHoraInicioAsc(
            idCliente, EstadoCita.programada, LocalDateTime.now()
        ).ifPresent(cita -> {
            dto.setProximaCita(citaMapper.toDto(cita));
        });
        
        return dto;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void agregarFamiliar(Integer idPropietario, Integer idBeneficiario, String relacion) {
        if (idPropietario.equals(idBeneficiario)) {
            throw new IllegalArgumentException("Un cliente no puede ser familiar de sí mismo.");
        }
        Cliente propietario = clienteRepository.findById(idPropietario)
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con id: " + idPropietario));

        Cliente beneficiario = clienteRepository.findById(idBeneficiario)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiario no encontrado con id: " + idBeneficiario));

        if (grupoFamiliarRepository.existsByPropietarioIdClienteAndBeneficiarioIdCliente(idPropietario, idBeneficiario)) {
            throw new IllegalArgumentException("Esta relación familiar ya existe.");
        }
        GrupoFamiliar grupo = new GrupoFamiliar();
        grupo.setPropietario(propietario);
        grupo.setBeneficiario(beneficiario);
        grupo.setRelacion(relacion);
        
        // REGLA 1: Asignar clínica explícitamente desde la entidad padre
        if (propietario.getClinica() != null) {
            grupo.setClinica(propietario.getClinica());
        }

        GrupoFamiliar guardado = grupoFamiliarRepository.save(grupo);

        String detalle = "Relación creada: " + propietario.getNombre() + " es propietario de -> " + beneficiario.getNombre() + " (Relación: " + relacion + ")";

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR,
                "GRUPO_FAMILIAR",
                guardado.getIdGrupo().toString(),
                detalle
        );
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void recoverCliente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.setActivo(true);
        clienteRepository.save(cliente);
        
        String detalle = "Paciente reactivado: " + cliente.getNombre() + " " + cliente.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.REACTIVAR,
                "CLIENTE",
                id.toString(),
                detalle
        );
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteFamiliar(Integer idGrupo) {
        GrupoFamiliar grupo = grupoFamiliarRepository.findById(idGrupo)
                .orElseThrow(() -> new ResourceNotFoundException("Relación familiar no encontrada"));

        grupoFamiliarRepository.deleteById(idGrupo);

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.ELIMINAR_FISICO,
                "GRUPO_FAMILIAR",
                idGrupo.toString(),
                "Relación eliminada entre Propietario: " + grupo.getPropietario().getNombre() +
                        " y Beneficiario: " + grupo.getBeneficiario().getNombre()
        );
    }

    private Cliente getClienteByIdEntity(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    @Override
    public void solicitarFirmaLopd(Integer idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente));

        String nombreCompleto = ((cliente.getNombre() != null ? cliente.getNombre() : "") + " " +
                (cliente.getApellidos() != null ? cliente.getApellidos() : "")).trim();

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("action", "OPEN_LOPD");
        payload.put("idCliente", idCliente);
        payload.put("nombrePaciente", nombreCompleto);

        messagingTemplate.convertAndSend("/topic/kiosk", payload);
    }

    public record LopdData(
            Long clinicaId,
            String nombreClinica,
            String nombrePaciente,
            String dniPaciente
    ) {}

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public LopdData extraerDatosLopd(Integer idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente));

        Long clinicaId = (cliente.getClinica() != null && cliente.getClinica().getIdClinica() != null)
                ? cliente.getClinica().getIdClinica()
                : 1L;

        String nombreClinica = (cliente.getClinica() != null && cliente.getClinica().getNombre() != null)
                ? cliente.getClinica().getNombre()
                : "Clínica Quiropráctica";

        String nombrePaciente = ((cliente.getNombre() != null ? cliente.getNombre() : "") + " " +
                (cliente.getApellidos() != null ? cliente.getApellidos() : "")).trim();

        String dniPaciente = (cliente.getDniNie() != null && !cliente.getDniNie().isBlank())
                ? cliente.getDniNie()
                : "No especificado";

        return new LopdData(clinicaId, nombreClinica, nombrePaciente, dniPaciente);
    }

    @Override
    public byte[] generarBorradorLopd(Integer idCliente) {
        LopdData datos = extraerDatosLopd(idCliente);
        return pdfFirmaService.generarPdfLopd(datos.nombreClinica(), datos.nombrePaciente(), datos.dniPaciente(), null);
    }

    @Override
    public java.util.Map<String, Object> firmarLopd(Integer idCliente, String firmaBase64) {
        if (firmaBase64 == null || firmaBase64.isBlank()) {
            throw new IllegalArgumentException("La firma en Base64 es obligatoria para rubricar la LOPD.");
        }

        // 1. Extraer los datos primitivos dentro de una transacción corta para evitar LazyInitializationException
        LopdData datos = extraerDatosLopd(idCliente);

        // 2. Generar el PDF final con la rúbrica en RAM (Zero-Storage previo a R2)
        byte[] pdfBytes = pdfFirmaService.generarPdfLopd(datos.nombreClinica(), datos.nombrePaciente(), datos.dniPaciente(), firmaBase64);

        // 3. Construir la ruta multi-tenant para R2 garantizando la extensión .pdf
        String baseName = String.format("consentimiento_lopd_%d_%d", idCliente, System.currentTimeMillis());
        String r2Key = storagePathBuilder.buildConsentimientosPath(datos.clinicaId(), idCliente, baseName, ".pdf");

        // 4. Subir a Cloudflare R2 (I/O de red FUERA de transacción para proteger el pool de conexiones)
        r2StorageService.storeBytes(pdfBytes, r2Key, "application/pdf");

        // 5. Micro-transacción SQL para persistir el documento, archivar LOPDs previas (Idempotencia) y actualizar cliente
        Cliente clienteActualizado = transactionTemplate.execute(status -> {
            Cliente c = clienteRepository.findById(idCliente)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente));

            c.setLopdFirmada(true);
            Cliente guardado = clienteRepository.save(c);

            // Idempotencia: Si ya existía algún consentimiento LOPD activo previo, se desactiva (archivado)
            List<DocumentoCliente> lopsPrevias = documentoClienteRepository
                    .findByClienteIdClienteAndTipoDocumentoAndActivoTrue(guardado.getIdCliente(), TipoDocumento.CONSENTIMIENTO_LOPD);
            for (DocumentoCliente previa : lopsPrevias) {
                previa.setActivo(false);
                previa.setFechaEliminacionLogica(LocalDateTime.now());
                previa.setEliminadoPor("RENOVACION_LOPD");
                documentoClienteRepository.save(previa);
            }

            DocumentoCliente doc = new DocumentoCliente();
            doc.setCliente(guardado);
            doc.setClinica(guardado.getClinica());
            doc.setNombreOriginal("Consentimiento_LOPD_" + guardado.getIdCliente() + ".pdf");
            doc.setPathArchivo(r2Key);
            doc.setTipoDocumento(TipoDocumento.CONSENTIMIENTO_LOPD);
            doc.setEtiqueta("Consentimiento LOPD");
            doc.setMimeType("application/pdf");
            doc.setTamanyoBytes((long) pdfBytes.length);
            doc.setEstadoSubida(EstadoSubida.ACTIVO);
            doc.setActivo(true);
            doc.setNotasMedicas("Consentimiento LOPD/RGPD firmado digitalmente desde el quiosco.");
            doc.setFechaCreacion(LocalDateTime.now());
            DocumentoCliente docGuardado = documentoClienteRepository.save(doc);

            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.CREAR,
                    "DOCUMENTO",
                    docGuardado.getIdDocumento().toString(),
                    "Consentimiento LOPD firmado y guardado en R2 para paciente: " + guardado.getNombre() + " " + guardado.getApellidos()
            );

            return guardado;
        });

        // 6. Notificar a Recepción y al quiosco que la LOPD ha sido firmada para actualizar el estado en tiempo real.
        // NOTA CRÍTICA: NO enviamos CLOSE_SIGNATURE para no cortar la previsualización de 5s del PDF en el quiosco.
        java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
        wsPayload.put("action", "LOPD_FIRMADA");
        wsPayload.put("idCliente", idCliente);
        messagingTemplate.convertAndSend("/topic/clientes", wsPayload);
        messagingTemplate.convertAndSend("/topic/kiosk", wsPayload);

        // 7. Preparar respuesta con DTO enriquecido y PDF firmado en Base64
        String signedPdfBase64 = java.util.Base64.getEncoder().encodeToString(pdfBytes);
        String signedPdfUrl = r2StorageService.generatePresignedUrl(r2Key);

        ClienteDto dto = enrichClienteDetalleDto(clienteActualizado != null ? clienteActualizado : clienteRepository.findById(idCliente).orElseThrow());

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("cliente", dto);
        response.put("signedPdfBase64", signedPdfBase64);
        response.put("signedPdfUrl", signedPdfUrl);

        return response;
    }
}
