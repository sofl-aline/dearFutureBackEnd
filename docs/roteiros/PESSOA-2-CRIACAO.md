# Pessoa 2 — criação de cápsulas

Pré-requisito único para começar: **etapa zero integrada na `main`**. Cada arquivo abaixo contém o código completo da etapa; não há métodos omitidos ou trechos que dependam de copiar o código de outro integrante. Seguir os commits na ordem deste guia.

No primeiro uso do clone, executar `sh scripts/generate-keys.sh` antes dos testes. Se as chaves já existem, não executar novamente: o script recusa sobrescrever. Não compartilhar chaves privadas no grupo.

## Preparar a branch

Substituir `seu-usuario` pelo login GitHub e executar na raiz do repositório de backend. Se a branch já existe, apenas selecioná-la, sem usar `-c`.

```sh
git switch main
git pull --ff-only origin main
git switch -c seu-usuario/criacao-capsulas
```

Os comandos de commit são executados **depois** de criar os arquivos e revisar os testes. Este roteiro não executa commits por você. Cada integrante usa seu próprio nome/e-mail Git. Não usar `git add .`: selecionar somente os arquivos da sua etapa.

Os testes isolados de controller usam `MockMvc` sem cadeia de segurança, com identidade explícita. Eles verificam contrato e delegação. A segurança real já está configurada na base e será exercitada pelos testes de autenticação e integração; não remover filtros da aplicação para fazer esses testes passarem.

## Commit 1 — `feat: criar capsulas com validacao e proprietario`

Criar request, caso de uso e testes sem iniciar servidor. Usuário vem do parâmetro do serviço; não há dependência do AuthService. Validation no HTTP cobre formato; serviço cobre regra temporal e validações necessárias para uso direto.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CreateCapsuleRequest.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleCreationService.java`
- `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleCreationServiceTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CreateCapsuleRequest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
public record CreateCapsuleRequest(@NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 10000) String message, @NotNull Instant opensAt) {
    public CreateCapsuleRequest {
        title = title == null ? null : title.trim();
        message = message == null ? null : message.trim();
    }
}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleCreationService.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.service;

import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.capsule.dto.*;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.mapper.CapsuleSummaryMapper;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import static br.edu.uniesp.dearfuture.shared.exception.ApiException.Code.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CapsuleCreationService {
    private final CapsuleRepository capsules;
    private final UserRepository users;
    private final Clock clock;
    public CapsuleCreationService(CapsuleRepository capsules, UserRepository users, Clock clock) {
        this.capsules = capsules; this.users = users; this.clock = clock;
    }
    @Transactional
    public CapsuleSummary create(UUID userId, CreateCapsuleRequest input) {
        var now = clock.instant();
        Map<String, String> errors = new LinkedHashMap<>();
        if (input.title() == null || input.title().isBlank() || input.title().length() > 120)
            errors.put("title", "Informe um título de até 120 caracteres.");
        if (input.message() == null || input.message().isBlank() || input.message().length() > 10000)
            errors.put("message", "Informe uma carta de até 10000 caracteres.");
        if (input.opensAt() == null || !input.opensAt().isAfter(now))
            errors.put("opensAt", "Escolha uma data e hora futuras.");
        if (!errors.isEmpty()) throw new ApiException(VALIDATION_ERROR, "Revise os campos informados.", errors);
        var owner = users.findById(userId).orElseThrow(() -> new ApiException(UNAUTHENTICATED, "Usuário não encontrado."));
        var capsule = new Capsule(UUID.randomUUID(), owner, input.title(), input.message(), now, input.opensAt());
        return CapsuleSummaryMapper.toSummary(capsules.save(capsule), now);
    }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleCreationServiceTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.capsule.dto.CreateCapsuleRequest;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleCreationService;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CapsuleCreationServiceTest {
    final CapsuleRepository capsules = mock(CapsuleRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final Instant now = Instant.parse("2030-01-01T12:00:00Z");
    final UUID owner = UUID.randomUUID();
    final CapsuleCreationService service = new CapsuleCreationService(capsules, users, Clock.fixed(now, ZoneOffset.UTC));
    @Test void createsUsingAuthenticatedOwnerAndServerTime() {
        when(users.findById(owner)).thenReturn(Optional.of(new User(owner, "A", "a@example.com", "hash", now)));
        when(capsules.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.create(owner, new CreateCapsuleRequest(" Título ", " Carta ", now.plusSeconds(60)));
        assertThat(result.title()).isEqualTo("Título");
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(result.status()).isEqualTo("SEALED");
        verify(users).findById(owner);
    }
    @Test void rejectsPastAndExactlyNow() {
        for (Instant date : List.of(now.minusSeconds(1), now)) {
            assertThatThrownBy(() -> service.create(owner, new CreateCapsuleRequest("T", "Carta", date)))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrors()).containsKey("opensAt"));
        }
        verifyNoInteractions(capsules);
    }
    @Test void rejectsBlankAndOversizedContent() {
        for (String title : List.of(" ", "a".repeat(121))) {
            assertThatThrownBy(() -> service.create(owner, new CreateCapsuleRequest(title, "Carta", now.plusSeconds(1))))
                    .isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> service.create(owner, new CreateCapsuleRequest("T", "a".repeat(10001), now.plusSeconds(1))))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(capsules);
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=CapsuleCreationServiceTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CreateCapsuleRequest.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleCreationService.java
git add src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleCreationServiceTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: criar capsulas com validacao e proprietario"
git push -u origin HEAD
```

Resultado esperado: build e testes sem falhas. Se falhar, corrigir antes do commit. O primeiro download exige rede. Os comandos `git diff` permitem conferir exatamente o conteúdo que será enviado.

## Commit 2 — `feat: expor criacao de capsulas pela api`

Criar o controller fino e seu teste HTTP isolado. Identidade vem de Authentication; corpo JSON não decide proprietário. O teste isolado não comprova a validação criptográfica do JWT: isso será verificado na integração.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleCreationController.java`
- `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleCreationControllerTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleCreationController.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.controller;

import br.edu.uniesp.dearfuture.internal.capsule.dto.*;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleCreationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleCreationController {
    private final CapsuleCreationService service;
    public CapsuleCreationController(CapsuleCreationService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<CapsuleSummary> create(Authentication authentication, @Valid @RequestBody CreateCapsuleRequest request) {
        var result = service.create(UUID.fromString(authentication.getName()), request);
        return ResponseEntity.created(URI.create("/api/v1/capsules/" + result.id())).body(result);
    }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleCreationControllerTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.capsule.controller.CapsuleCreationController;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleCreationService;
import br.edu.uniesp.dearfuture.internal.capsule.dto.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CapsuleCreationControllerTest {
    @Test void delegatesWithAuthenticatedIdentity() throws Exception {
        var service = mock(CapsuleCreationService.class);
        var owner = UUID.randomUUID();
        var id = UUID.randomUUID();
        var now = Instant.parse("2030-01-01T12:00:00Z");
        when(service.create(eq(owner), any())).thenReturn(new CapsuleSummary(id, "T", now, now.plusSeconds(60), "SEALED"));
        var mvc = MockMvcBuilders.standaloneSetup(new CapsuleCreationController(service)).build();
        var principal = new UsernamePasswordAuthenticationToken(owner.toString(), "unused");
        mvc.perform(post("/api/v1/capsules").principal(principal).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"T\",\"message\":\"Carta\",\"opensAt\":\"2030-01-01T12:01:00Z\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/capsules/" + id)).andExpect(jsonPath("$.message").doesNotExist());
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=CapsuleCreationControllerTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleCreationController.java
git add src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleCreationControllerTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: expor criacao de capsulas pela api"
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

Validação de formato versus regra temporal, transação de criação, dados gerados pelo servidor e proprietário obtido da autenticação.
