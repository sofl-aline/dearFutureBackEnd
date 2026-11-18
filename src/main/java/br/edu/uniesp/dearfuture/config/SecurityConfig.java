package br.edu.uniesp.dearfuture.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
@Configuration
public class SecurityConfig {
    @Bean public SecurityFilterChain security(HttpSecurity http, ProblemResponses problems) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            problems.write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                    "Autenticação necessária ou token inválido.", request.getRequestURI());
        };
        AccessDeniedHandler forbidden = (request, response, exception) -> problems.write(response,
                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Acesso negado.", request.getRequestURI());
        http.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/me", "/api/v1/capsules", "/api/v1/capsules/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
            .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults())
                .authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden));
        return http.build();
    }
}
