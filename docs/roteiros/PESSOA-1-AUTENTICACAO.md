# Pessoa 1 — cadastro, login e identidade

Pré-requisito único para começar: **etapa zero integrada na `main`**. Cada arquivo abaixo contém o código completo da etapa; não há métodos omitidos ou trechos que dependam de copiar o código de outro integrante. Seguir os commits na ordem deste guia.

No primeiro uso do clone, executar `sh scripts/generate-keys.sh` antes dos testes. Se as chaves já existem, não executar novamente: o script recusa sobrescrever. Não compartilhar chaves privadas no grupo.

## Preparar a branch

Substituir `seu-usuario` pelo login GitHub e executar na raiz do repositório de backend. Se a branch já existe, apenas selecioná-la, sem usar `-c`.

```sh
git switch main
git pull --ff-only origin main
git switch -c seu-usuario/autenticacao
```

Os comandos de commit são executados **depois** de criar os arquivos e revisar os testes. Este roteiro não executa commits por você. Cada integrante usa seu próprio nome/e-mail Git. Não usar `git add .`: selecionar somente os arquivos da sua etapa.

Os testes isolados de controller usam `MockMvc` sem cadeia de segurança, com identidade explícita. Eles verificam contrato e delegação. A segurança real já está configurada na base e será exercitada pelos testes de autenticação e integração; não remover filtros da aplicação para fazer esses testes passarem.

## Commit 1 — `feat: implementar cadastro e autenticacao de usuarios`

Criar os DTOs, o provider de autenticação e o serviço. Usar os models, chaves, encoder e configuração JWT já entregues na etapa zero. Este commit não depende do handler MVC da Pessoa 5 nem dos endpoints de cápsulas.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/RegisterRequest.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/LoginRequest.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/UserResponse.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/TokenResponse.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/config/AuthenticationConfig.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/service/AuthService.java`
- `src/test/java/br/edu/uniesp/dearfuture/auth/AuthServiceTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/RegisterRequest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;
public record RegisterRequest(@NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 12, max = 128) String password) {
    public RegisterRequest {
        name = name == null ? null : name.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/LoginRequest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;
public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 12, max = 128) String password) {
    public LoginRequest { email = email == null ? null : email.trim().toLowerCase(Locale.ROOT); }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/UserResponse.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.dto;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import java.time.Instant;
import java.util.UUID;
public record UserResponse(UUID id, String name, String email, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/TokenResponse.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/config/AuthenticationConfig.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.config;

import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class AuthenticationConfig {
    @Bean public UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(user -> User.withUsername(user.getEmail()).password(user.getPasswordHash())
                        .authorities("USER").build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }
    @Bean public AuthenticationManager authenticationManager(UserDetailsService details, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/service/AuthService.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.service;

import br.edu.uniesp.dearfuture.internal.auth.dto.*;
import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import static br.edu.uniesp.dearfuture.shared.exception.ApiException.Code.*;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authentication;
    private final JwtEncoder jwt;
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final long ttl;
    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authentication,
            JwtEncoder jwt, Clock clock, @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience, @Value("${app.jwt.ttl-seconds}") long ttl) {
        this.users = users; this.encoder = encoder; this.authentication = authentication;
        this.jwt = jwt; this.clock = clock; this.issuer = issuer; this.audience = audience; this.ttl = ttl;
    }
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByEmail(request.email())) throw duplicateEmail();
        var user = new User(UUID.randomUUID(), request.name(), request.email(),
                encoder.encode(request.password()), clock.instant());
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException exception) {
            // A checagem anterior não impede dois cadastros simultâneos do mesmo e-mail.
            // Não continuar usando a transação após a violação.
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && violation.getConstraintName().toLowerCase(java.util.Locale.ROOT).contains("uk_users_email")) {
                    throw duplicateEmail();
                }
                cause = cause.getCause();
            }
            throw exception;
        }
        return UserResponse.from(user);
    }
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        try { authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())); }
        catch (AuthenticationException exception) { throw new ApiException(INVALID_CREDENTIALS, "E-mail ou senha inválidos."); }
        var user = users.findByEmail(request.email()).orElseThrow(() -> new ApiException(INVALID_CREDENTIALS, "E-mail ou senha inválidos."));
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(user.getId().toString())
                .audience(List.of(audience)).issuedAt(now).expiresAt(now.plusSeconds(ttl)).build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return new TokenResponse(jwt.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), "Bearer", ttl);
    }
    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return users.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> new ApiException(UNAUTHENTICATED, "Usuário não encontrado."));
    }
    private ApiException duplicateEmail() { return new ApiException(EMAIL_ALREADY_REGISTERED, "E-mail já cadastrado."); }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/auth/AuthServiceTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.auth;

import br.edu.uniesp.dearfuture.internal.auth.dto.*;
import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.auth.service.AuthService;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthServiceTest {
    final UserRepository users = mock(UserRepository.class);
    final PasswordEncoder encoder = mock(PasswordEncoder.class);
    final AuthenticationManager manager = mock(AuthenticationManager.class);
    final JwtEncoder jwt = mock(JwtEncoder.class);
    final Clock clock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);
    final AuthService service = new AuthService(users, encoder, manager, jwt, clock, "dearfuture", "dearfuture-api", 900);
    @Test void normalizesEmailAndStoresEncodedPassword() {
        when(encoder.encode("senha-longa-de-teste")).thenReturn("hash");
        var result = service.register(new RegisterRequest(" Ana ", " ANA@EXAMPLE.COM ", "senha-longa-de-teste"));
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(result.email()).isEqualTo("ana@example.com");
        assertThat(result.name()).isEqualTo("Ana");
    }
    @Test void rejectsDuplicateEmailWithoutSaving() {
        when(users.existsByEmail("ana@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "ana@example.com", "senha-longa-de-teste")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.EMAIL_ALREADY_REGISTERED));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void concurrentDuplicateIsTranslated() {
        when(encoder.encode(anyString())).thenReturn("hash");
        when(users.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("fixture",
                new org.hibernate.exception.ConstraintViolationException("fixture", new java.sql.SQLException("fixture"), "uk_users_email")));
        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "ana@example.com", "senha-longa-de-teste")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.EMAIL_ALREADY_REGISTERED));
    }
    @Test void invalidCredentialsDoNotIssueToken() {
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("fixture"));
        assertThatThrownBy(() -> service.login(new LoginRequest("ana@example.com", "senha-longa-de-teste")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.INVALID_CREDENTIALS));
        verifyNoInteractions(jwt);
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=AuthServiceTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/RegisterRequest.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/LoginRequest.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/UserResponse.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/dto/TokenResponse.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/config/AuthenticationConfig.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/service/AuthService.java
git add src/test/java/br/edu/uniesp/dearfuture/auth/AuthServiceTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: implementar cadastro e autenticacao de usuarios"
git push -u origin HEAD
```

Resultado esperado: build e testes sem falhas. Se falhar, corrigir antes do commit. O primeiro download exige rede. Os comandos `git diff` permitem conferir exatamente o conteúdo que será enviado.

## Commit 2 — `feat: expor endpoints de cadastro login e perfil`

Criar o controller e verificar cadastro/login/me com token assinado de verdade. Erros de negócio terão o formato definitivo após o PR da Pessoa 5; os testes desta etapa já cobrem sucesso e bloqueios da cadeia de segurança.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/internal/auth/controller/AuthController.java`
- `src/test/java/br/edu/uniesp/dearfuture/auth/AuthHttpTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/auth/controller/AuthController.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.auth.controller;

import br.edu.uniesp.dearfuture.internal.auth.dto.*;
import br.edu.uniesp.dearfuture.internal.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) { return service.me(UUID.fromString(authentication.getName())); }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/auth/AuthHttpTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.auth;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthHttpTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Test void registerLoginAndMeUseRealSignedToken() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String registration = json.writeValueAsString(java.util.Map.of("name", "Ana", "email", email, "password", "senha-longa-de-teste"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        String body = json.writeValueAsString(java.util.Map.of("email", email, "password", "senha-longa-de-teste"));
        var response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expiresIn").value(900)).andReturn();
        String token = json.readTree(response.getResponse().getContentAsString()).get("accessToken").asString();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
    @Test void missingTokenReturnsJsonNotHtml() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=AuthHttpTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/internal/auth/controller/AuthController.java
git add src/test/java/br/edu/uniesp/dearfuture/auth/AuthHttpTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: expor endpoints de cadastro login e perfil"
git push -u origin HEAD
```

Resultado esperado: build e testes sem falhas. Se falhar, corrigir antes do commit. O primeiro download exige rede. Os comandos `git diff` permitem conferir exatamente o conteúdo que será enviado.

## Revisão e aceite

Abrir PR para `main` depois de uma parte revisável, sem esperar terminar tudo. Informar o que mudou, o teste executado e qualquer dependência. Depois do merge do primeiro PR, sincronizar a branch antes do próximo:

```sh
git fetch origin
git merge origin/main
```

As alterações de telas React serão feitas em PRs próprios no repositório do frontend, depois da base de cliente HTTP/autenticação. Este guia entrega o **backend completo da sua responsabilidade**; não altera a versão atual do frontend baseada em localStorage. O contrato necessário para essa integração está no planejamento e no `docs/api.md` da Pessoa 5.

## O que explicar na apresentação

Cadastro com hash e salt, diferença entre autenticação e autorização, emissão/validação JWT, identidade em sub, expiração e limite do logout sem revogação.
