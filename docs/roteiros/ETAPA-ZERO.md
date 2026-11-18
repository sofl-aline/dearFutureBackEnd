# Etapa zero — base comum, feita por você

Responsável: você. Esta etapa é feita uma única vez, antes de distribuir as funcionalidades. Os trechos abaixo são código de implementação, não apenas uma lista de tarefas. Copiar cada bloco para o caminho indicado, a partir da raiz do backend.

Requisitos: Git, JDK 21, Maven instalado para gerar o wrapper e OpenSSL. No macOS, conferir `java -version` e `mvn -version`: ambos precisam apontar para Java 21. Se necessário, selecionar o JDK nas configurações do terminal/IDE antes de continuar. O wrapper fixa Maven 3.9.11 e os códigos usam Boot 4.1.1, como no projeto de referência da aula.

A base inclui a configuração completa de segurança para não bloquear os colegas. As chaves são locais e geradas pelo script; seu conteúdo nunca entra nos guias ou commits. Uma chave existente não é sobrescrita. O script precisa ser executado apenas na primeira preparação de cada clone.

## Preparar a branch

Substituir `seu-usuario` pelo login GitHub e executar na raiz do repositório de backend. Se a branch já existe, apenas selecioná-la, sem usar `-c`.

```sh
git switch main
git pull --ff-only origin main
git switch -c seu-usuario/base-backend
```

Os comandos de commit são executados **depois** de criar os arquivos e revisar os testes. Este roteiro não executa commits por você. Cada integrante usa seu próprio nome/e-mail Git. Não usar `git add .`: selecionar somente os arquivos da sua etapa.

## Commit 1 — `chore: configurar base compartilhada do backend`

Criar a aplicação e toda a base que os cinco integrantes reutilizarão. O usuário executa esta etapa inteira, inclusive entidade, repositories, política temporal, mapeamento e segurança. Não há endpoint funcional de cadastro/login/cápsulas ainda; isso é esperado.

Arquivos desta etapa:

- `pom.xml`
- `src/main/java/br/edu/uniesp/dearfuture/DearFutureApplication.java`
- `.gitignore`
- `src/main/resources/application.yaml`
- `src/main/resources/db/migration/V1__create_users.sql`
- `src/main/resources/db/migration/V2__create_capsules.sql`
- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/entity/User.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/repository/UserRepository.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/entity/Capsule.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/repository/CapsuleRepository.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CapsuleSummary.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/policy/CapsuleAvailability.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/mapper/CapsuleSummaryMapper.java`
- `src/main/java/br/edu/uniesp/dearfuture/shared/exception/ApiException.java`
- `src/main/java/br/edu/uniesp/dearfuture/config/ProblemResponses.java`
- `src/main/java/br/edu/uniesp/dearfuture/config/CoreConfig.java`
- `src/main/java/br/edu/uniesp/dearfuture/config/JwtConfig.java`
- `src/main/java/br/edu/uniesp/dearfuture/config/SecurityConfig.java`
- `scripts/generate-keys.sh`
- `src/test/resources/application-test.yaml`
- `src/test/java/br/edu/uniesp/dearfuture/base/BasePersistenceTest.java`
- `README.md`

### Arquivo: `pom.xml`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
    <relativePath/>
  </parent>
  <groupId>br.edu.uniesp</groupId>
  <artifactId>dearfuture-backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties><java.version>21</java.version></properties>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-resource-server</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-flyway</artifactId></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build><plugins>
    <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId>
      <configuration>
        <argLine>-javaagent:"${settings.localRepository}/org/mockito/mockito-core/${mockito.version}/mockito-core-${mockito.version}.jar"</argLine>
      </configuration>
    </plugin>
  </plugins></build>
</project>
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/DearFutureApplication.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class DearFutureApplication {
    public static void main(String[] args) {
        SpringApplication.run(DearFutureApplication.class, args);
    }
}
```

### Arquivo: `.gitignore`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```text
.env
.env.*
!.env.example
.idea/
.vscode/
.DS_Store
*.log
target/
data/
secrets/
```

### Arquivo: `src/main/resources/application.yaml`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```yaml
spring:
  application:
    name: dearfuture
  datasource:
    url: ${DB_URL:jdbc:h2:file:./data/dearfuture}
    username: ${DB_USERNAME:sa}
    password: ${DB_PASSWORD:}
  jackson:
    deserialization:
      fail-on-unknown-properties: true
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  h2:
    console:
      enabled: false
server:
  port: ${SERVER_PORT:8080}
app:
  cors:
    origins: ${CORS_ORIGINS:http://localhost:5173}
  jwt:
    public-key: ${JWT_PUBLIC_KEY:file:./secrets/public.pem}
    private-key: ${JWT_PRIVATE_KEY:file:./secrets/private.pem}
    issuer: dearfuture
    audience: dearfuture-api
    ttl-seconds: 900
```

### Arquivo: `src/main/resources/db/migration/V1__create_users.sql`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```sql
CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);
```

### Arquivo: `src/main/resources/db/migration/V2__create_capsules.sql`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```sql
CREATE TABLE capsules (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(10000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    opens_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_capsules_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
    CONSTRAINT ck_capsules_future CHECK (opens_at > created_at)
);
CREATE INDEX ix_capsules_owner_created ON capsules(owner_id, created_at);
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/entity/User.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Pattern;
@Entity
@Table(name = "app_users")
public class User {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 254, unique = true) private String email;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected User() {}
    public User(UUID id, String name, String email, String passwordHash, Instant createdAt) {
        this.id = java.util.Objects.requireNonNull(id);
        changeDisplayName(name);
        if (email == null) throw new IllegalArgumentException("E-mail obrigatório.");
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > 254 || !EMAIL.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        this.email = normalizedEmail;
        if (passwordHash == null || passwordHash.isBlank() || passwordHash.length() > 255) {
            throw new IllegalArgumentException("Hash da senha obrigatório e limitado a 255 caracteres.");
        }
        this.passwordHash = passwordHash;
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
    }
    public void changeDisplayName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new IllegalArgumentException("Nome obrigatório e limitado a 120 caracteres.");
        }
        this.name = name.trim();
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/repository/UserRepository.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.repository;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/entity/Capsule.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.entity;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "capsules")
public class Capsule {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 10000) private String message;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "opens_at", nullable = false) private Instant opensAt;
    protected Capsule() {}
    public Capsule(UUID id, User owner, String title, String message, Instant createdAt, Instant opensAt) {
        this.id = java.util.Objects.requireNonNull(id);
        this.owner = java.util.Objects.requireNonNull(owner);
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
        this.opensAt = java.util.Objects.requireNonNull(opensAt);
        if (title == null || title.isBlank() || title.trim().length() > 120
                || message == null || message.isBlank() || message.trim().length() > 10000
                || !opensAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Dados inválidos para a cápsula.");
        }
        this.title = title.trim();
        this.message = message.trim();
    }
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getOpensAt() { return opensAt; }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/repository/CapsuleRepository.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.repository;

import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CapsuleRepository extends JpaRepository<Capsule, UUID> {
    List<Capsule> findAllByOwner_IdOrderByCreatedAtDescIdAsc(UUID ownerId);
    Optional<Capsule> findByIdAndOwner_Id(UUID id, UUID ownerId);
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CapsuleSummary.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.dto;

import java.time.Instant;
import java.util.UUID;
public record CapsuleSummary(UUID id, String title, Instant createdAt, Instant opensAt, String status) {}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/policy/CapsuleAvailability.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.policy;

import java.time.Instant;
public final class CapsuleAvailability {
    private CapsuleAvailability() {}
    public static boolean isAvailable(Instant opensAt, Instant now) { return !now.isBefore(opensAt); }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/mapper/CapsuleSummaryMapper.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.mapper;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleSummary;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.policy.CapsuleAvailability;
import java.time.Instant;
public final class CapsuleSummaryMapper {
    private CapsuleSummaryMapper() {}
    public static CapsuleSummary toSummary(Capsule capsule, Instant now) {
        return new CapsuleSummary(capsule.getId(), capsule.getTitle(), capsule.getCreatedAt(),
                capsule.getOpensAt(), CapsuleAvailability.isAvailable(capsule.getOpensAt(), now)
                ? "AVAILABLE" : "SEALED");
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/shared/exception/ApiException.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.shared.exception;

import java.util.Map;
public class ApiException extends RuntimeException {
    public enum Code { VALIDATION_ERROR, CAPSULE_NOT_FOUND, CAPSULE_SEALED,
        EMAIL_ALREADY_REGISTERED, INVALID_CREDENTIALS, UNAUTHENTICATED, ACCESS_DENIED }
    private final Code code;
    private final Map<String, String> errors;
    public ApiException(Code code, String message) { this(code, message, Map.of()); }
    public ApiException(Code code, String message, Map<String, String> errors) {
        super(message);
        this.code = code;
        this.errors = Map.copyOf(errors);
    }
    public Code getCode() { return code; }
    public Map<String, String> getErrors() { return errors; }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/config/ProblemResponses.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
@Component
public class ProblemResponses {
    private final ObjectMapper mapper;
    public ProblemResponses(ObjectMapper mapper) { this.mapper = mapper; }
    public ProblemDetail create(HttpStatus status, String code, String detail, String path,
                                Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(path));
        problem.setProperty("code", code);
        problem.setProperty("erros", errors);
        return problem;
    }
    public void write(HttpServletResponse response, HttpStatus status, String code,
                      String detail, String path) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", path);
        body.put("code", code);
        body.put("erros", Map.of());
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), body);
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/config/CoreConfig.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.config;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.*;
import org.springframework.web.cors.*;
@Configuration
public class CoreConfig {
    @Bean public Clock clock() { return Clock.systemUTC(); }
    @Bean public PasswordEncoder passwordEncoder() {
        var pbkdf2 = new Pbkdf2PasswordEncoder("", 16, 600000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        return new DelegatingPasswordEncoder("pbkdf2", Map.of("pbkdf2", pbkdf2));
    }
    @Bean public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origins}") List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/config/JwtConfig.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
@Configuration
public class JwtConfig {
    @Bean public RSAPublicKey publicKey(@Value("${app.jwt.public-key}") Resource resource) throws IOException {
        try (var input = resource.getInputStream()) { return RsaKeyConverters.x509().convert(input); }
    }
    @Bean public RSAPrivateKey privateKey(@Value("${app.jwt.private-key}") Resource resource) throws IOException {
        try (var input = resource.getInputStream()) { return RsaKeyConverters.pkcs8().convert(input); }
    }
    @Bean public JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("dearfuture-local").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
    @Bean public JwtDecoder jwtDecoder(RSAPublicKey publicKey,
            @Value("${app.jwt.issuer}") String issuer, @Value("${app.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        OAuth2TokenValidator<Jwt> subjectValidator = jwt -> {
            try { java.util.UUID.fromString(jwt.getSubject()); return OAuth2TokenValidatorResult.success(); }
            catch (IllegalArgumentException | NullPointerException e) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator, subjectValidator));
        return decoder;
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/config/SecurityConfig.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
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
```

### Arquivo: `scripts/generate-keys.sh`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```sh
#!/usr/bin/env sh
set -eu
umask 077
mkdir -p secrets
if [ -e secrets/private.pem ] || [ -e secrets/public.pem ]; then
  echo "Já existem chaves. Nada foi sobrescrito."
  exit 1
fi
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/private.pem
openssl pkey -in secrets/private.pem -pubout -out secrets/public.pem
echo "Chaves locais criadas em secrets/. Não adicionar ao Git."
```

### Arquivo: `src/test/resources/application-test.yaml`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:dearfuture;DB_CLOSE_DELAY=-1
    username: sa
    password:
app:
  jwt:
    public-key: file:./secrets/public.pem
    private-key: file:./secrets/private.pem
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/base/BasePersistenceTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.base;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BasePersistenceTest {
    @Autowired UserRepository users;
    @Autowired CapsuleRepository capsules;
    @Test void migrationAndOwnershipWork() {
        var now = Instant.parse("2026-11-18T12:00:00Z");
        var a = users.saveAndFlush(new User(UUID.randomUUID(), "A", "a-base@example.com", "hash-fixture", now));
        var b = users.saveAndFlush(new User(UUID.randomUUID(), "B", "b-base@example.com", "hash-fixture", now));
        var capsule = capsules.saveAndFlush(new Capsule(UUID.randomUUID(), a, "Título", "Carta", now, now.plusSeconds(60)));
        assertThat(capsules.findByIdAndOwner_Id(capsule.getId(), a.getId())).isPresent();
        assertThat(capsules.findByIdAndOwner_Id(capsule.getId(), b.getId())).isEmpty();
        assertThat(capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(b.getId())).isEmpty();
    }
}
```

### Arquivo: `README.md`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

````markdown
# DearFuture — Backend

API de cápsulas do tempo privadas por usuário. Frontend: https://github.com/sofl-aline/dearFuture.

## Executar

Requisitos: JDK 21, OpenSSL e acesso à internet para baixar dependências na primeira execução.

```sh
java -version
sh scripts/generate-keys.sh
./mvnw verify
./mvnw spring-boot:run
```

A geração das chaves é feita uma vez por clone e recusa sobrescrever arquivos existentes.
No Windows, usar Git Bash para o script e `mvnw.cmd` para Maven.
Se ainda estiver montando a etapa zero, gerar o wrapper com Maven instalado:

```sh
mvn -N org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper -Dmaven=3.9.11 -Dtype=only-script
```

API: `http://localhost:8080/api/v1`. Frontend permitido: `http://localhost:5173`.
Banco local em `data/`, preservado após reinícios. Testes usam H2 em memória.
Não apagar `data/` para resolver migrações sem entender a consequência para os dados.

## Configuração

| Variável | Padrão |
| --- | --- |
| `SERVER_PORT` | `8080` |
| `DB_URL` | `jdbc:h2:file:./data/dearfuture` |
| `DB_USERNAME` | `sa` |
| `DB_PASSWORD` | vazia, somente para demonstração local |
| `CORS_ORIGINS` | `http://localhost:5173` (múltiplas origens separadas por vírgula) |
| `JWT_PUBLIC_KEY` | `file:./secrets/public.pem` |
| `JWT_PRIVATE_KEY` | `file:./secrets/private.pem` |

Spring não carrega um `.env` automaticamente nesta configuração. Exportar variáveis no terminal ou configurar a IDE.
Não versionar chaves privadas, tokens, cartas reais, senhas ou o banco.
Testes locais também exigem executar o script de chaves. O CI gera chaves descartáveis.

## Autenticação e privacidade

Cadastro, login e endpoints entram nos PRs de funcionalidades após a base.
Todas as rotas de cápsulas exigem Bearer JWT. A base já bloqueia acesso anônimo.
Tokens duram 15 minutos, sem refresh token. O frontend deve manter token somente em memória.
Logout no frontend descarta token e dados; token previamente copiado permanece válido até expirar.
Sem recuperação de senha, administração ou compartilhamento no MVP.
A carta só é liberada ao proprietário no instante de abertura ou depois dele.

O contrato completo será publicado em `docs/api.md` pela Pessoa 5.
A validação deve incluir duas contas e chamada direta à API, além da interface.
A demonstração local não deve ser publicada sem HTTPS e controles adicionais de acesso/login.

## Equipe

Preencher os cinco nomes completos e logins antes da entrega.
Cada integrante trabalha em branch própria, com commits individuais e PRs revisados.
````

### Validar e publicar o commit

O wrapper gera automaticamente `mvnw`, `mvnw.cmd` e `.mvn/wrapper/maven-wrapper.properties`; não é preciso escrever o código desses arquivos de infraestrutura à mão. No Windows, usar Git Bash para OpenSSL e `mvnw.cmd` nos comandos Maven.

```sh
mvn -N org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper -Dmaven=3.9.11 -Dtype=only-script
chmod +x mvnw
sh scripts/generate-keys.sh
./mvnw -Dtest=BasePersistenceTest test
./mvnw verify
git add pom.xml
git add src/main/java/br/edu/uniesp/dearfuture/DearFutureApplication.java
git add .gitignore
git add src/main/resources/application.yaml
git add src/main/resources/db/migration/V1__create_users.sql
git add src/main/resources/db/migration/V2__create_capsules.sql
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/entity/User.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/repository/UserRepository.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/entity/Capsule.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/repository/CapsuleRepository.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CapsuleSummary.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/policy/CapsuleAvailability.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/mapper/CapsuleSummaryMapper.java
git add src/main/java/br/edu/uniesp/dearfuture/shared/exception/ApiException.java
git add src/main/java/br/edu/uniesp/dearfuture/config/ProblemResponses.java
git add src/main/java/br/edu/uniesp/dearfuture/config/CoreConfig.java
git add src/main/java/br/edu/uniesp/dearfuture/config/JwtConfig.java
git add src/main/java/br/edu/uniesp/dearfuture/config/SecurityConfig.java
git add scripts/generate-keys.sh
git add src/test/resources/application-test.yaml
git add src/test/java/br/edu/uniesp/dearfuture/base/BasePersistenceTest.java
git add README.md
git add mvnw
git add mvnw.cmd
git add .mvn/wrapper/maven-wrapper.properties
git diff --cached --check
git diff --cached
git commit -m "chore: configurar base compartilhada do backend"
git push -u origin HEAD
```

Resultado esperado: build e testes sem falhas. Se falhar, corrigir antes do commit. O primeiro download exige rede. Os comandos `git diff` permitem conferir exatamente o conteúdo que será enviado.

## Revisão e aceite

Abrir PR da base para `main`, pedir revisão e integrar preservando o commit. Só então as cinco pessoas criam suas branches. Avisar que o banco local está vazio e que as rotas funcionais ainda serão adicionadas. O README é uma base; completar os nomes reais depois.

Nesta base, `GET /api/v1/capsules` sem token retorna `401` mesmo sem controller. Isso mostra que acesso anônimo está bloqueado; não demonstra que a listagem já existe.

Os testes usam o perfil `test` e H2 em memória. Para executar localmente, usar `./mvnw spring-boot:run`; o padrão H2 em arquivo persiste dados em `data/`. Não executar o perfil `test` na demonstração de persistência.

## O que explicar na apresentação

Fluxo HTTP → controller → service → repository → banco, migrações, identidade do proprietário e por que a base compartilhada libera os commits individuais.
