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
