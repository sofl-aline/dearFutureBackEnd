# Pessoa 3 — listagem, detalhe e persistência

Pré-requisito único para começar: **etapa zero integrada na `main`**. Cada arquivo abaixo contém o código completo da etapa; não há métodos omitidos ou trechos que dependam de copiar o código de outro integrante. Seguir os commits na ordem deste guia.

No primeiro uso do clone, executar `sh scripts/generate-keys.sh` antes dos testes. Se as chaves já existem, não executar novamente: o script recusa sobrescrever. Não compartilhar chaves privadas no grupo.

## Preparar a branch

Substituir `seu-usuario` pelo login GitHub e executar na raiz do repositório de backend. Se a branch já existe, apenas selecioná-la, sem usar `-c`.

```sh
git switch main
git pull --ff-only origin main
git switch -c seu-usuario/consultas-capsulas
```

Os comandos de commit são executados **depois** de criar os arquivos e revisar os testes. Este roteiro não executa commits por você. Cada integrante usa seu próprio nome/e-mail Git. Não usar `git add .`: selecionar somente os arquivos da sua etapa.

Os testes isolados de controller usam `MockMvc` sem cadeia de segurança, com identidade explícita. Eles verificam contrato e delegação. A segurança real já está configurada na base e será exercitada pelos testes de autenticação e integração; não remover filtros da aplicação para fazer esses testes passarem.

## Commit 1 — `feat: consultar capsulas por proprietario`

Criar consultas e testes de banco sem chamar o cadastro ou POST de cápsulas. Entidade, migration e repository já existem: não recriar esses arquivos. O desempate da ordenação usa UUID crescente.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleQueryService.java`
- `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleQueryServiceTest.java`
- `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleRepositoryTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleQueryService.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.service;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleSummary;
import br.edu.uniesp.dearfuture.internal.capsule.mapper.CapsuleSummaryMapper;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import static br.edu.uniesp.dearfuture.shared.exception.ApiException.Code.CAPSULE_NOT_FOUND;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class CapsuleQueryService {
    private final CapsuleRepository capsules;
    private final Clock clock;
    public CapsuleQueryService(CapsuleRepository capsules, Clock clock) { this.capsules = capsules; this.clock = clock; }
    public List<CapsuleSummary> list(UUID userId) {
        var now = clock.instant();
        return capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(userId).stream()
                .map(capsule -> CapsuleSummaryMapper.toSummary(capsule, now)).toList();
    }
    public CapsuleSummary get(UUID userId, UUID id) {
        var capsule = capsules.findByIdAndOwner_Id(id, userId)
                .orElseThrow(() -> new ApiException(CAPSULE_NOT_FOUND, "Cápsula não encontrada."));
        return CapsuleSummaryMapper.toSummary(capsule, clock.instant());
    }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleQueryServiceTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleQueryService;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CapsuleQueryServiceTest {
    final CapsuleRepository capsules = mock(CapsuleRepository.class);
    final Instant now = Instant.parse("2030-01-01T12:00:00Z");
    final UUID owner = UUID.randomUUID();
    final CapsuleQueryService service = new CapsuleQueryService(capsules, Clock.fixed(now, ZoneOffset.UTC));
    @Test void listsOnlyByAuthenticatedOwnerAndCalculatesAvailability() {
        var user = new User(owner, "A", "a@example.com", "hash", now.minusSeconds(60));
        var capsule = new Capsule(UUID.randomUUID(), user, "T", "Carta", now.minusSeconds(60), now);
        when(capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(owner)).thenReturn(List.of(capsule));
        assertThat(service.list(owner)).singleElement().satisfies(c -> assertThat(c.status()).isEqualTo("AVAILABLE"));
        verify(capsules).findAllByOwner_IdOrderByCreatedAtDescIdAsc(owner);
    }
    @Test void missingOrOtherOwnersCapsuleReturnsSameException() {
        UUID id = UUID.randomUUID();
        when(capsules.findByIdAndOwner_Id(id, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(owner, id)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.CAPSULE_NOT_FOUND));
        verify(capsules, never()).findById(any());
    }
    @Test void emptyListIsNotError() {
        when(capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(owner)).thenReturn(List.of());
        assertThat(service.list(owner)).isEmpty();
    }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleRepositoryTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.capsule;

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
class CapsuleRepositoryTest {
    @Autowired UserRepository users;
    @Autowired CapsuleRepository capsules;
    @Test void newestFirstAndOtherAccountExcluded() {
        var now = Instant.parse("2030-01-01T12:00:00Z");
        var a = users.saveAndFlush(new User(UUID.randomUUID(), "A", UUID.randomUUID()+"@example.com", "hash", now));
        var b = users.saveAndFlush(new User(UUID.randomUUID(), "B", UUID.randomUUID()+"@example.com", "hash", now));
        var older = capsules.saveAndFlush(new Capsule(UUID.randomUUID(), a, "Antiga", "Carta", now, now.plusSeconds(600)));
        var newer = capsules.saveAndFlush(new Capsule(UUID.randomUUID(), a, "Nova", "Carta", now.plusSeconds(1), now.plusSeconds(600)));
        capsules.saveAndFlush(new Capsule(UUID.randomUUID(), b, "De B", "Carta", now.plusSeconds(2), now.plusSeconds(600)));
        assertThat(capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(a.getId()))
                .extracting(Capsule::getId).containsExactly(newer.getId(), older.getId());
        assertThat(capsules.findByIdAndOwner_Id(newer.getId(), b.getId())).isEmpty();
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=CapsuleQueryServiceTest,CapsuleRepositoryTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleQueryService.java
git add src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleQueryServiceTest.java
git add src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleRepositoryTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: consultar capsulas por proprietario"
git push -u origin HEAD
```

Resultado esperado: build e testes sem falhas. Se falhar, corrigir antes do commit. O primeiro download exige rede. Os comandos `git diff` permitem conferir exatamente o conteúdo que será enviado.

## Commit 2 — `feat: expor listagem e detalhe de capsulas`

Criar controller de consultas separado do controller de criação. Retornar somente metadados. Lista vazia é sucesso; ID alheio usa a mesma falha de ID inexistente.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleQueryController.java`
- `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleQueryControllerTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleQueryController.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.controller;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleSummary;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleQueryController {
    private final CapsuleQueryService service;
    public CapsuleQueryController(CapsuleQueryService service) { this.service = service; }
    @GetMapping
    public List<CapsuleSummary> list(Authentication authentication) {
        return service.list(UUID.fromString(authentication.getName()));
    }
    @GetMapping("/{id}")
    public CapsuleSummary get(Authentication authentication, @PathVariable UUID id) {
        return service.get(UUID.fromString(authentication.getName()), id);
    }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleQueryControllerTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.capsule.controller.CapsuleQueryController;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleQueryService;
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
class CapsuleQueryControllerTest {
    @Test void delegatesWithAuthenticatedIdentity() throws Exception {
        var service = mock(CapsuleQueryService.class);
        var owner = UUID.randomUUID();
        var id = UUID.randomUUID();
        var now = Instant.parse("2030-01-01T12:00:00Z");
        when(service.get(owner, id)).thenReturn(new CapsuleSummary(id, "T", now, now.plusSeconds(60), "SEALED"));
        var mvc = MockMvcBuilders.standaloneSetup(new CapsuleQueryController(service)).build();
        var principal = new UsernamePasswordAuthenticationToken(owner.toString(), "unused");
        mvc.perform(get("/api/v1/capsules/" + id).principal(principal))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").doesNotExist());
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=CapsuleQueryControllerTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleQueryController.java
git add src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleQueryControllerTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: expor listagem e detalhe de capsulas"
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

Consultas por proprietário no banco, ordenação, transações de leitura, DTO sem carta e diferença entre inexistente e alheio na implementação.
