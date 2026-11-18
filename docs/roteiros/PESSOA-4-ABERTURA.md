# Pessoa 4 — abertura autorizada da carta

Pré-requisito único para começar: **etapa zero integrada na `main`**. Cada arquivo abaixo contém o código completo da etapa; não há métodos omitidos ou trechos que dependam de copiar o código de outro integrante. Seguir os commits na ordem deste guia.

No primeiro uso do clone, executar `sh scripts/generate-keys.sh` antes dos testes. Se as chaves já existem, não executar novamente: o script recusa sobrescrever. Não compartilhar chaves privadas no grupo.

## Preparar a branch

Substituir `seu-usuario` pelo login GitHub e executar na raiz do repositório de backend. Se a branch já existe, apenas selecioná-la, sem usar `-c`.

```sh
git switch main
git pull --ff-only origin main
git switch -c seu-usuario/abertura-capsulas
```

Os comandos de commit são executados **depois** de criar os arquivos e revisar os testes. Este roteiro não executa commits por você. Cada integrante usa seu próprio nome/e-mail Git. Não usar `git add .`: selecionar somente os arquivos da sua etapa.

Os testes isolados de controller usam `MockMvc` sem cadeia de segurança, com identidade explícita. Eles verificam contrato e delegação. A segurança real já está configurada na base e será exercitada pelos testes de autenticação e integração; não remover filtros da aplicação para fazer esses testes passarem.

## Commit 1 — `feat: validar proprietario e horario para ler cartas`

Criar DTO da carta, serviço e testes de horário. Buscar por ID e proprietário antes de comparar datas; assim o usuário alheio não descobre o estado de abertura.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CapsuleMessage.java`
- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleOpeningService.java`
- `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleOpeningServiceTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CapsuleMessage.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.dto;

import java.util.UUID;
public record CapsuleMessage(UUID id, String message) {}
```

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleOpeningService.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.service;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleMessage;
import br.edu.uniesp.dearfuture.internal.capsule.policy.CapsuleAvailability;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import static br.edu.uniesp.dearfuture.shared.exception.ApiException.Code.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CapsuleOpeningService {
    private final CapsuleRepository capsules;
    private final Clock clock;
    public CapsuleOpeningService(CapsuleRepository capsules, Clock clock) { this.capsules = capsules; this.clock = clock; }
    @Transactional(readOnly = true)
    public CapsuleMessage read(UUID userId, UUID id) {
        var capsule = capsules.findByIdAndOwner_Id(id, userId)
                .orElseThrow(() -> new ApiException(CAPSULE_NOT_FOUND, "Cápsula não encontrada."));
        if (!CapsuleAvailability.isAvailable(capsule.getOpensAt(), clock.instant()))
            throw new ApiException(CAPSULE_SEALED, "A cápsula ainda está selada.");
        return new CapsuleMessage(capsule.getId(), capsule.getMessage());
    }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleOpeningServiceTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleOpeningService;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CapsuleOpeningServiceTest {
    final CapsuleRepository capsules = mock(CapsuleRepository.class);
    final Instant opens = Instant.parse("2030-01-01T12:00:00Z");
    final UUID owner = UUID.randomUUID(), id = UUID.randomUUID();
    final User user = new User(owner, "A", "a@example.com", "hash", opens.minusSeconds(60));
    final Capsule capsule = new Capsule(id, user, "T", "Carta secreta", opens.minusSeconds(60), opens);
    CapsuleOpeningService at(Instant now) { return new CapsuleOpeningService(capsules, Clock.fixed(now, ZoneOffset.UTC)); }
    @Test void blocksOneNanosecondBeforeOpening() {
        when(capsules.findByIdAndOwner_Id(id, owner)).thenReturn(Optional.of(capsule));
        assertThatThrownBy(() -> at(opens.minusNanos(1)).read(owner, id)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.CAPSULE_SEALED));
    }
    @Test void releasesExactlyAtOpeningAndAfter() {
        when(capsules.findByIdAndOwner_Id(id, owner)).thenReturn(Optional.of(capsule));
        assertThat(at(opens).read(owner, id).message()).isEqualTo("Carta secreta");
        assertThat(at(opens.plusSeconds(1)).read(owner, id).message()).isEqualTo("Carta secreta");
    }
    @Test void otherOwnerNeverGetsContentOrTimingInformation() {
        UUID other = UUID.randomUUID();
        when(capsules.findByIdAndOwner_Id(id, other)).thenReturn(Optional.empty());
        for (Instant now : List.of(opens.minusSeconds(1), opens.plusSeconds(1))) {
            assertThatThrownBy(() -> at(now).read(other, id)).isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.CAPSULE_NOT_FOUND));
        }
        verify(capsules, never()).findById(any());
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=CapsuleOpeningServiceTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/dto/CapsuleMessage.java
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/service/CapsuleOpeningService.java
git add src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleOpeningServiceTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: validar proprietario e horario para ler cartas"
git push -u origin HEAD
```

Resultado esperado: build e testes sem falhas. Se falhar, corrigir antes do commit. O primeiro download exige rede. Os comandos `git diff` permitem conferir exatamente o conteúdo que será enviado.

## Commit 2 — `feat: expor leitura protegida da carta`

Criar controller de abertura e teste de delegação. A carta só pode ser obtida nesta rota, após autorização e horário. O contador do frontend não participa da decisão.

Arquivos desta etapa:

- `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleOpeningController.java`
- `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleOpeningControllerTest.java`

### Arquivo: `src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleOpeningController.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.internal.capsule.controller;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleMessage;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleOpeningService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleOpeningController {
    private final CapsuleOpeningService service;
    public CapsuleOpeningController(CapsuleOpeningService service) { this.service = service; }
    @GetMapping("/{id}/message")
    public CapsuleMessage read(Authentication authentication, @PathVariable UUID id) {
        return service.read(UUID.fromString(authentication.getName()), id);
    }
}
```

### Arquivo: `src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleOpeningControllerTest.java`

Criar este arquivo com o conteúdo completo abaixo. Se for um arquivo já existente da configuração inicial, substituir pelo conteúdo completo indicado.

```java
package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.capsule.controller.CapsuleOpeningController;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleOpeningService;
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
class CapsuleOpeningControllerTest {
    @Test void delegatesWithAuthenticatedIdentity() throws Exception {
        var service = mock(CapsuleOpeningService.class);
        var owner = UUID.randomUUID();
        var id = UUID.randomUUID();
        var now = Instant.parse("2030-01-01T12:00:00Z");
        when(service.read(owner, id)).thenReturn(new CapsuleMessage(id, "Carta"));
        var mvc = MockMvcBuilders.standaloneSetup(new CapsuleOpeningController(service)).build();
        var principal = new UsernamePasswordAuthenticationToken(owner.toString(), "unused");
        mvc.perform(get("/api/v1/capsules/" + id + "/message").principal(principal))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Carta"));
    }
}
```

### Validar e publicar o commit

```sh
./mvnw -Dtest=CapsuleOpeningControllerTest test
./mvnw verify
git add src/main/java/br/edu/uniesp/dearfuture/internal/capsule/controller/CapsuleOpeningController.java
git add src/test/java/br/edu/uniesp/dearfuture/capsule/CapsuleOpeningControllerTest.java
git diff --cached --check
git diff --cached
git commit -m "feat: expor leitura protegida da carta"
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

Verificação de propriedade antes do horário, Clock fixo nos testes, instante exato de liberação e ausência de tarefa agendada.
