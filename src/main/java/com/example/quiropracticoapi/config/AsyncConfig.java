package com.example.quiropracticoapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("AuditoriaAsync-");
        
        // El TaskDecorator se encarga de pasar el contexto de seguridad del hilo principal al hilo @Async
        executor.setTaskDecorator(new SecurityContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    private static class SecurityContextTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Se captura en el hilo principal antes de lanzar el asíncrono
            SecurityContext securityContext = SecurityContextHolder.getContext();
            Long tenantId = com.example.quiropracticoapi.config.TenantContext.getTenantId();
            
            return () -> {
                try {
                    // Ya en el nuevo hilo, restauramos ambos contextos
                    SecurityContextHolder.setContext(securityContext);
                    if (tenantId != null) {
                        com.example.quiropracticoapi.config.TenantContext.setTenantId(tenantId);
                    }
                    runnable.run();
                } finally {
                    SecurityContextHolder.clearContext();
                    com.example.quiropracticoapi.config.TenantContext.clear();
                }
            };
        }
    }
}
