package com.habitacheck.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var matcher = PathPatternRequestMatcher.withDefaults();

        // HU-registro: crear cuenta
        var registro = matcher.matcher(HttpMethod.POST, "/api/auth/registro");

        // HU-foto-perfil: perfil completo (temporal hasta JWT)
        var perfilGet    = matcher.matcher(HttpMethod.GET,    "/api/perfil");
        var perfilPatch  = matcher.matcher(HttpMethod.PATCH,  "/api/perfil");
        var perfilFotoPut = matcher.matcher(HttpMethod.PUT,   "/api/perfil/foto");
        var perfilFotoDel = matcher.matcher(HttpMethod.DELETE, "/api/perfil/foto");

        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        registro, perfilGet, perfilPatch, perfilFotoPut, perfilFotoDel))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(registro).permitAll()
                        // TODO: reemplazar por autenticación JWT cuando esté lista
                        .requestMatchers(perfilGet, perfilPatch, perfilFotoPut, perfilFotoDel)
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
