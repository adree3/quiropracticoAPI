package com.example.quiropracticoapi.config;

import com.example.quiropracticoapi.model.Auditoria;
import com.example.quiropracticoapi.model.SoftDeletable;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.service.impl.AuditoriaServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Field;

@Component
public class AuditEventListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final EntityManagerFactory entityManagerFactory;
    private final AuditoriaServiceImpl auditoriaService;
    private final ObjectMapper mapper;

    @Autowired
    public AuditEventListener(EntityManagerFactory entityManagerFactory, @Lazy AuditoriaServiceImpl auditoriaService) {
        this.entityManagerFactory = entityManagerFactory;
        this.auditoriaService = auditoriaService;
        
        // Configuramos el mapper para evitar ciclos infinitos al serializar Lazy Proxies
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule()); // Para fechas de Java 8+
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // Usamos nuestro MixIn para ignorar las cabeceras de Hibernate
        this.mapper.addMixIn(Object.class, HibernateProxyMixIn.class);
    }

    @PostConstruct
    private void registerListeners() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        
        // Registramos en Append para que se ejecute después de cualquier lógica principal
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        logAction(event.getEntity(), TipoAccion.CREAR);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Object entity = event.getEntity();
        if (entity instanceof Auditoria) return;

        // Detección de Soft Delete completamente genérica, sin cadenas hardcodeadas.
        // Si la entidad implementa SoftDeletable, le preguntamos directamente si está eliminada.
        // El estado anterior se refleja en el hecho de que el evento es un UPDATE.
        TipoAccion accion = TipoAccion.EDITAR;
        if (entity instanceof SoftDeletable softDeletable && softDeletable.isEliminadoLogico()) {
            accion = TipoAccion.ELIMINAR_LOGICO;
        }

        logAction(entity, accion);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        logAction(event.getEntity(), TipoAccion.ELIMINAR_FISICO);
    }

    private void logAction(Object entity, TipoAccion tipoAccion) {
        if (entity instanceof Auditoria) return;

        Class<?> clazz = entity.getClass();
        // Quitar "$HibernateProxy$" u otros sufijos que mete CGLIB si se llama el método en lazies
        String entidadNombre = clazz.getSimpleName().split("\\$")[0];
        String isIdStr = obtenerId(entity);
        
        // Convertimos a JSON previniendo infinite recursion por relaciones bidireccionales
        String detalles = "{}";
        try {
            detalles = mapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            detalles = "{\"error\": \"No se pudo serializar la entidad\"}";
        }

        auditoriaService.registrarAccion(tipoAccion, entidadNombre, isIdStr, detalles);
    }

    private String obtenerId(Object entity) {
        try {
            // Buscamos dinámicamente el campo ID por la anotación o de forma aproximada
            Field[] fields = entity.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                    field.setAccessible(true);
                    return String.valueOf(field.get(entity));
                }
            }
        } catch (Exception e) {
            // Fallback silencioso si no logra leer por reflection
        }
        return "N/A";
    }
}
