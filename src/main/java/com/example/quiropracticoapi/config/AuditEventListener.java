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

        TipoAccion accion = TipoAccion.EDITAR;

        // Solo las entidades con borrado lógico necesitan la comparación de transición
        if (entity instanceof SoftDeletable) {
            // Comparamos el estado ANTERIOR vs el NUEVO usando los arrays de Hibernate.
            // Esto evita el edge case de registrar ELIMINAR_LOGICO si la entidad ya estaba
            // eliminada y solo se editó otro campo.
            String[] propertyNames = event.getPersister().getPropertyNames();
            Object[] oldState = event.getOldState();
            Object[] newState = event.getState();

            boolean eraEliminado = isEliminadoEnEstado(entity, propertyNames, oldState);
            boolean esEliminado  = isEliminadoEnEstado(entity, propertyNames, newState);

            if (!eraEliminado && esEliminado) {
                accion = TipoAccion.ELIMINAR_LOGICO;
            } else if (eraEliminado && !esEliminado) {
                accion = TipoAccion.REACTIVAR;
            }
            // Si ambos son iguales → EDITAR (valor por defecto)
        }

        logAction(entity, accion);
    }

    /**
     * Determina si la entidad estaba en estado "eliminado lógicamente" según un snapshot del estado de Hibernate.
     * Delega en la implementación concreta de SoftDeletable a través de reflection temporal sobre el estado dado.
     * Para evitar tener que instanciar una entidad nueva, comparamos los valores de los campos clave directamente.
     */
    private boolean isEliminadoEnEstado(Object entity, String[] propertyNames, Object[] estado) {
        if (estado == null) return false;
        // Estrategia: buscamos los campos que SoftDeletable usa para determinar el borrado lógico.
        // Cubrimos los dos patrones de nuestro dominio:
        //  1. Campo 'activo' (boolean) → eliminado cuando activo = false
        //  2. Campo 'estado' con valor 'cancelada' (enum EstadoCita)
        for (int i = 0; i < propertyNames.length; i++) {
            if ("activo".equals(propertyNames[i])) {
                return Boolean.FALSE.equals(estado[i]);
            }
            if ("estado".equals(propertyNames[i]) && estado[i] != null) {
                return "cancelada".equalsIgnoreCase(estado[i].toString());
            }
        }
        return false;
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
