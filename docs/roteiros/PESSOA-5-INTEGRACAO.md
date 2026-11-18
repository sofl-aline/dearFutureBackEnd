# Pessoa 5 — erros, contrato, CI e integração

Pré-requisito único para começar: **etapa zero integrada na `main`**. Cada arquivo abaixo contém o código completo da etapa; não há métodos omitidos ou trechos que dependam de copiar o código de outro integrante. Seguir os commits na ordem deste guia.

No primeiro uso do clone, executar `sh scripts/generate-keys.sh` antes dos testes. Se as chaves já existem, não executar novamente: o script recusa sobrescrever. Não compartilhar chaves privadas no grupo.

## Preparar a branch

Substituir `seu-usuario` pelo login GitHub e executar na raiz do repositório de backend. Se a branch já existe, apenas selecioná-la, sem usar `-c`.

```sh
git switch main
git pull --ff-only origin main
git switch -c seu-usuario/contrato-integracao
```

Os comandos de commit são executados **depois** de criar os arquivos e revisar os testes. Este roteiro não executa commits por você. Cada integrante usa seu próprio nome/e-mail Git. Não usar `git add .`: selecionar somente os arquivos da sua etapa.

Os testes isolados de controller usam `MockMvc` sem cadeia de segurança, com identidade explícita. Eles verificam contrato e delegação. A segurança real já está configurada na base e será exercitada pelos testes de autenticação e integração; não remover filtros da aplicação para fazer esses testes passarem.

## Commit 1 — `feat: padronizar erros e headers da api`

Criar o advice MVC e o filtro de Cache-Control. O escritor ProblemResponses e a configuração inicial CORS já estão na etapa zero: reutilizar, sem trocar o contrato dos colegas. Este teste usa controller local de teste, sem dependência das outras pessoas.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/config/exception/GlobalExceptionHandler.java`
- `src/main/java/br/edu/uniesp/dearfuture/config/ApiHeadersFilter.java`
- `src/test/java/br/edu/uniesp/dearfuture/config/GlobalExceptionHandlerTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/config/exception/GlobalExceptionHandler.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.config.exception;

import br.edu.uniesp.dearfuture.config.ProblemResponses;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemResponses problems;
    public GlobalExceptionHandler(ProblemResponses problems) { this.problems = problems; }
    @ExceptionHandler(ApiException.class)
    public ProblemDetail business(ApiException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case CAPSULE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CAPSULE_SEALED, EMAIL_ALREADY_REGISTERED -> HttpStatus.CONFLICT;
            case INVALID_CREDENTIALS, UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
        };
        return problems.create(status, exception.getCode().name(), exception.getMessage(),
                request.getRequestURI(), exception.getErrors());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return problems.create(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Revise os campos informados.", request.getRequestURI(), errors);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail malformed(Exception exception, HttpServletRequest request) {
        return problems.create(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Requisição inválida.", request.getRequestURI(), Map.of());
    }
    @ExceptionHandler(org.springframework.web.ErrorResponseException.class)
    public ProblemDetail framework(org.springframework.web.ErrorResponseException exception, HttpServletRequest request) {
        return problems.create(HttpStatus.valueOf(exception.getStatusCode().value()), "HTTP_ERROR",
                "Não foi possível atender esta requisição.", request.getRequestURI(), Map.of());
    }
    @ExceptionHandler({org.springframework.web.HttpRequestMethodNotSupportedException.class,
            org.springframework.web.HttpMediaTypeNotSupportedException.class,
            org.springframework.web.servlet.resource.NoResourceFoundException.class})
    public ProblemDetail protocol(Exception exception, HttpServletRequest request) {
        var error = (org.springframework.web.ErrorResponse) exception;
        return problems.create(HttpStatus.valueOf(error.getStatusCode().value()), "HTTP_ERROR",
                "Método, conteúdo ou recurso não disponível.", request.getRequestURI(), Map.of());
    }
    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        // Não registrar a exceção inteira: mensagens de SQL/conversão podem conter cartas ou senhas.
        String event = java.util.UUID.randomUUID().toString();
        LOG.error("Falha interna: evento={}, tipo={}", event, exception.getClass().getName());
        return problems.create(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Falha interna. Referência: " + event, request.getRequestURI(), Map.of());
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/config/ApiHeadersFilter.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiHeadersFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/")) response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/config/GlobalExceptionHandlerTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.config;

import br.edu.uniesp.dearfuture.config.exception.GlobalExceptionHandler;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class GlobalExceptionHandlerTest {
    @RestController static class FixtureController {
        @GetMapping("/fixture/missing") String missing() { throw new ApiException(ApiException.Code.CAPSULE_NOT_FOUND, "Cápsula não encontrada."); }
        @GetMapping("/fixture/sealed") String sealed() { throw new ApiException(ApiException.Code.CAPSULE_SEALED, "A cápsula ainda está selada."); }
        @GetMapping("/fixture/bug") String bug() { throw new IllegalStateException("NAO_EXPOR_CARTA"); }
    }
    @Test void translatesErrorsWithoutExposingUnexpectedDetails() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FixtureController())
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemResponses(JsonMapper.builder().build()))).build();
        mvc.perform(get("/fixture/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAPSULE_NOT_FOUND"));
        mvc.perform(get("/fixture/sealed")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAPSULE_SEALED"));
        mvc.perform(get("/fixture/bug")).andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("NAO_EXPOR_CARTA"))));
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=GlobalExceptionHandlerTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/config/exception/GlobalExceptionHandler.java
git add src/main/java/br/edu/uniesp/dearfuture/config/ApiHeadersFilter.java
git add src/test/java/br/edu/uniesp/dearfuture/config/GlobalExceptionHandlerTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: padronizar erros e headers da api"
git push -u origin HEAD
```

Resultado esperado: build e testes sem falhas. Se falhar, corrigir antes do commit. O primeiro download exige rede. Os comandos `git diff` permitem conferir exatamente o conteúdo que será enviado.

## Commit 2 — `ci: verificar backend e documentar contrato http`

Criar workflow e contrato completo da API. CI gera chaves descartáveis antes dos testes; não copiar chaves da máquina do desenvolvedor. O contrato permite o frontend avançar com respostas simuladas.

Arquivos desta etapa:

- `.github/workflows/backend.yml`
- `docs/api.md`

### Arquivo: `.github/workflows/backend.yml`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```yaml
name: Backend
on:
  push:
    branches: [main]
  pull_request:
permissions:
  contents: read
jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Gerar chaves descartáveis de teste
        run: sh scripts/generate-keys.sh
      - name: Verificar
        run: ./mvnw -B verify
```

### Arquivo: `docs/api.md`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

````markdown
# DearFuture — contrato HTTP

Base: `http://localhost:8080/api/v1`. JSON UTF-8; datas ISO 8601 com offset explícito ou Z.
Headers de resposta de API: `Cache-Control: no-store`.

## Autenticação

`POST /auth/register` recebe:

```json
{"name":"Ana","email":"ana@example.com","password":"senha-longa-de-teste"}
```

Resposta `201`: `{id,name,email,createdAt}`. Não autentica automaticamente nem retorna hash.
Nome: 1–120 caracteres após trim. E-mail: válido, até 254; normalizado para minúsculas e trim.
Senha: 12–128 caracteres, sem trim; persistida como hash PBKDF2 com salt aleatório.
E-mail duplicado: `409 EMAIL_ALREADY_REGISTERED`.

`POST /auth/login` recebe `{email,password}` e responde:

```json
{"accessToken":"<JWT recebido do servidor>","tokenType":"Bearer","expiresIn":900}
```

Credenciais inválidas: `401 INVALID_CREDENTIALS`, mesma mensagem para e-mail inexistente ou senha errada.
Usar `Authorization: Bearer <token>` nas demais operações.
`GET /auth/me`: `200 {id,name,email,createdAt}` do usuário autenticado.
Token ausente, inválido, adulterado ou expirado: `401 UNAUTHENTICATED`.
Não existe endpoint de logout: a SPA remove token e dados da memória. Tokens copiados expiram em até 15 minutos; não há revogação imediata.

## Cápsulas

Todos os endpoints exigem autenticação e limitam dados ao proprietário. Campos desconhecidos são rejeitados com `400`; o cliente não envia `ownerId`, `id` ou `createdAt` na criação.

| Operação | Resposta de sucesso |
| --- | --- |
| `POST /capsules` | `201`, metadados e `Location: /api/v1/capsules/{id}` |
| `GET /capsules` | `200`, array de metadados da conta, `[]` quando vazio |
| `GET /capsules/{id}` | `200`, metadados |
| `GET /capsules/{id}/message` | `200`, `{id,message}`, somente a partir de `opensAt` |

Criação:

```json
{"title":"Para o futuro","message":"Minha carta","opensAt":"2030-01-01T12:00:00Z"}
```

O exemplo exige que 2030 ainda esteja no futuro ao testar; escolher outro instante se necessário.
Título: 1–120; mensagem: 1–10000 caracteres, após trim. Data: estritamente futura em relação ao servidor.
Metadados nunca incluem `message`, nem depois de a cápsula ficar disponível:

```json
{"id":"658bfb42-a1c8-4eb6-b742-ab4876dfbf76","title":"Para o futuro","createdAt":"2026-11-18T12:00:00Z","opensAt":"2030-01-01T12:00:00Z","status":"SEALED"}
```

Status calculado: `SEALED` antes e `AVAILABLE` exatamente no horário e depois.
Listagem por `createdAt` decrescente e UUID crescente para desempate. Sem paginação no MVP local.
UUID inválido: `400`. ID inexistente ou alheio: sempre `404 CAPSULE_NOT_FOUND`, inclusive no endpoint da carta. Cápsula própria antes da data: `409 CAPSULE_SEALED`.
A propriedade é verificada antes da regra temporal. Frontend não recebe nem filtra dados de outras contas.

## Erros

`Content-Type: application/problem+json`:

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Revise os campos informados.","instance":"/api/v1/capsules","code":"VALIDATION_ERROR","erros":{"opensAt":"Escolha uma data e hora futuras."}}
```

A aplicação deve tratar `code`, `status` e `erros`; não depender do texto de `title`/`detail` para decidir o comportamento.

| Status | Code | Uso |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Campos inválidos ou data não futura |
| 400 | `INVALID_REQUEST` | JSON, data, campo desconhecido ou UUID inválido |
| 401 | `INVALID_CREDENTIALS` | Falha no login |
| 401 | `UNAUTHENTICATED` | Token ausente/inválido ou identidade inexistente |
| 403 | `ACCESS_DENIED` | Rota não permitida para identidade autenticada |
| 404 | `CAPSULE_NOT_FOUND` | Cápsula inexistente ou alheia |
| 409 | `CAPSULE_SEALED` | Carta própria ainda bloqueada |
| 409 | `EMAIL_ALREADY_REGISTERED` | Cadastro duplicado |
| 405/415/404 | `HTTP_ERROR` | Falhas de protocolo/recurso tratadas pelo MVC |
| 500 | `INTERNAL_ERROR` | Falha inesperada; resposta sem dados internos |

Erros da cadeia de segurança são escritos fora do advice MVC, com o mesmo formato.
O login não retorna redirecionamento HTML. `401` da cadeia de filtros tem `WWW-Authenticate: Bearer`.
CORS permite somente origens configuradas e os headers Authorization/Content-Type, sem cookies.
Preflight não exige token; uma resposta 401 deve continuar visível à origem permitida.

## Integração futura com React

Substituir o serviço síncrono localStorage por Promises. Tipos distintos para metadados e carta.
Manter token somente em memória. Em 401 protegido, limpar autenticação, dados e consultas pendentes.
Evitar respostas atrasadas da conta anterior ao trocar usuário. Não repetir automaticamente POST.
Contador é apenas visual; tratar 409 caso o relógio local habilite o botão antes do servidor.
BrasilAPI permanece independente do fluxo de leitura. Não usar fallback silencioso para localStorage.
````

### Validar e publicar o commit

```sh
./mvnw verify
git add .github/workflows/backend.yml
git add docs/api.md
git diff --cached --check
git diff --cached
git commit -m "ci: verificar backend e documentar contrato http"
git push -u origin HEAD
```

Resultado esperado: build e testes sem falhas. Se falhar, corrigir antes do commit. O primeiro download exige rede. Os comandos `git diff` permitem conferir exatamente o conteúdo que será enviado.

## Commit 3 — `test: validar autenticacao e fluxo completo de capsulas`

Esta é a única etapa que aguarda os PRs de funcionalidades das Pessoas 1 a 4 e seus dois commits anteriores. Sincronizar com main após eles entrarem. Testar login real, isolamento, datas e erros de ponta a ponta no backend.

Antes de criar os arquivos desta etapa, na sua branch:

```sh
git fetch origin
git merge origin/main
```

Arquivos desta etapa:

- `src/test/java/br/edu/uniesp/dearfuture/integration/CompleteFlowTest.java`

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/integration/CompleteFlowTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.integration;

import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompleteFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired CapsuleRepository capsules;
    @Autowired JwtEncoder encoder;
    @MockitoBean Clock clock;
    Instant now;
    @BeforeEach void setup() {
        capsules.deleteAll(); users.deleteAll();
        now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        when(clock.instant()).thenReturn(now);
    }
    String account(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Pessoa", "email", email, "password", "senha-longa-de-teste"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).startsWith("{pbkdf2}").isNotEqualTo("senha-longa-de-teste");
        var result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "senha-longa-de-teste"))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }
    String create(String token) throws Exception {
        var input = Map.of("title", "T", "message", "Carta secreta", "opensAt", now.plusSeconds(120).toString());
        var result = mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }
    @Test void isolatesAccountsAndChecksExactOpeningTime() throws Exception {
        String a = account("a@example.com"), b = account("b@example.com");
        String id = create(a);
        mvc.perform(get("/api/v1/capsules").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].message").doesNotExist());
        mvc.perform(get("/api/v1/capsules").header("Authorization", "Bearer " + b)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/v1/capsules/" + id).header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/capsules/" + id + "/message").header("Authorization", "Bearer " + b))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAPSULE_NOT_FOUND"));
        mvc.perform(get("/api/v1/capsules/" + id + "/message").header("Authorization", "Bearer " + a))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAPSULE_SEALED"));
        when(clock.instant()).thenReturn(now.plusSeconds(120));
        mvc.perform(get("/api/v1/capsules/" + id + "/message").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Carta secreta"));
        mvc.perform(get("/api/v1/capsules/" + id + "/message").header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
    }
    @Test void duplicateAndInvalidCredentialsHavePredictableErrors() throws Exception {
        account("a@example.com");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "A", "email", "a@example.com", "password", "senha-longa-de-teste"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        for (String email : List.of("a@example.com", "missing@example.com")) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("email", email, "password", "senha-incorreta-longa"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
        }
    }
    @Test void invalidJsonUuidAndDateWithoutOffsetAreRejected() throws Exception {
        String token = account("a@example.com");
        for (String date : List.of("2030-01-01T00:00:00", "invalid")) {
            mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("title", "T", "message", "Carta", "opensAt", date))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "T", "message", "Carta", "opensAt", now.plusSeconds(120).toString(), "ownerId", UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/capsules/not-uuid").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
    @Test void rejectsExpiredWrongAudienceAndMalformedTokens() throws Exception {
        mvc.perform(get("/api/v1/capsules")).andExpect(status().isUnauthorized());
        for (boolean expired : List.of(true, false)) {
            var claims = JwtClaimsSet.builder().issuer("dearfuture").subject(UUID.randomUUID().toString())
                    .audience(List.of(expired ? "dearfuture-api" : "wrong"))
                    .issuedAt(now.minusSeconds(7200)).expiresAt(expired ? now.minusSeconds(3600) : now.plusSeconds(900)).build();
            String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
            mvc.perform(get("/api/v1/capsules").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
        mvc.perform(get("/api/v1/capsules").header("Authorization", "Bearer broken"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
    @Test void preflightAndErrorsAllowFrontendOrigin() throws Exception {
        mvc.perform(options("/api/v1/capsules").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(get("/api/v1/capsules").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=CompleteFlowTest test
./mvnw verify
git add src/test/java/br/edu/uniesp/dearfuture/integration/CompleteFlowTest.java
git diff --cached --check
git diff --cached
git commit -m "test: validar autenticacao e fluxo completo de capsulas"
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

Erros MVC e erros da cadeia de filtros, ProblemDetail, CORS, CI e testes de duas contas sem vazamento da carta.
