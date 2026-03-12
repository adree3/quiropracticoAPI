package com.example.quiropracticoapi.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * MixIn genérico para evitar errores de serialización (StackOverflowError/Infinite Recursion)
 * y proxies de Hibernate al convertir entidades a JSON en los logs de auditoría.
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class HibernateProxyMixIn {
    // Las propiedades "hibernateLazyInitializer" y "handler" son inyectadas
    // por el proxy de Hibernate. Ignorarlas es clave para que ObjectMapper no explote.
}
