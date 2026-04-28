package com.example.quiropracticoapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // El interceptor se aplica a todas las peticiones de la API
        // pero solo actuará si hay un usuario autenticado en el SecurityContext
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/api/**");
    }
}
