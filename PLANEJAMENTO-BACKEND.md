# DearFuture — planejamento e divisão do backend

**Roteiros de implementação:** [índice com etapa zero e cinco partes](docs/roteiros/README.md). Você fará toda a etapa zero; os integrantes começam suas funcionalidades após essa base. Os roteiros incluem código completo, testes e comandos para cada commit.

## 1. Resultado da análise

O arquivo recebido, `DIVISAO.md`, não corresponde ao DearFuture: organiza um sistema de assistência técnica com `Cliente`, `Equipamento`, `OrdemServico`, `Tecnico`, orçamento, peças e estoque. Essas entidades e regras não aparecem nas funcionalidades do projeto e não devem ser implementadas aqui.

Além disso, o documento concentra a integração em uma pessoa e distribui principalmente entidades. Entidades isoladas não entregam um backend utilizável pelo frontend: faltam contratos HTTP, persistência, validações, respostas de erro, configuração de acesso pelo navegador e testes do fluxo completo.

Este planejamento foi elaborado a partir do código e dos documentos locais do frontend em 18/11/2026: `README.md`, `docs/PLANO.md`, `src/types/capsule.ts`, `src/services/capsuleStorage.ts` e telas existentes. Os documentos descrevem funcionalidades previstas; parte delas ainda está em desenvolvimento no código consultado.

Os nomes dos integrantes e o enunciado da disciplina de backend não foram fornecidos. Por isso, mantemos Pessoas 1 a 5 na mesma numeração do plano do frontend. A stack foi alinhada aos slides e ao repositório do professor, conforme a seção 13; os requisitos específicos da avaliação ainda precisam ser confirmados. Confirmar com o professor stack, prazo, quantidade mínima de entidades e diagramas exigidos antes de iniciar a implementação. Não criar entidades sem função apenas para reproduzir o anexo.

## 2. Objetivo e limites da primeira entrega

Entregar uma API com cadastro, login e cápsulas privadas por usuário. A pessoa autenticada poderá criar uma cápsula com título, carta e data futura; listar cápsulas; consultar os metadados de uma cápsula; e ler a carta somente a partir do horário permitido pelo servidor. Os dados devem permanecer após reiniciar a aplicação.

O frontend React/TypeScript continua no repositório `sofl-aline/dearFuture`. O backend fica em `sofl-aline/dearFutureBackEnd`. A demonstração inicial será local, com os dois processos em execução.

### Incluído

- Cadastro, login, consulta do usuário autenticado e saída da conta no frontend.
- Criação, listagem, detalhe e leitura da carta somente pelo proprietário.
- Persistência em banco, validação de entradas e erros padronizados.
- Contrato de API documentado e integração futura com o frontend.
- Bloqueio da carta pelo servidor antes de `opensAt`.
- Testes de regras, persistência e endpoints.

### Fora do MVP

- Recuperação de senha, confirmação de e-mail, login social e perfis administrativos.
- Refresh token e revogação antecipada de tokens no servidor.
- Upload de fotos, anexos, notificações por e-mail e tarefas agendadas.
- Edição e exclusão de cápsulas, compartilhamento e personalização.
- Migração automática do localStorage.
- Intermediação da BrasilAPI pelo backend.

Cada conta terá sua própria coleção. Estar autenticado não basta: consultas, detalhe e leitura devem sempre limitar o acesso ao proprietário. A carta exige duas condições simultâneas: usuário dono da cápsula e horário de abertura alcançado.

Os feriados continuam sendo consultados pelo frontend, como previsto no trabalho atual. Essa consulta é independente da API de cápsulas e sua falha não pode impedir a leitura de uma carta.

## 3. Arquitetura proposta

Java 21 com Spring Boot, Maven Wrapper, Spring MVC, Bean Validation, Spring Data JPA e Spring Security. O `pom.xml` do professor consultado declara Spring Boot 4.1.1 e Java 21; usar essa combinação como referência de aula e confirmar sua resolução pelo Maven antes de fixar a base. Não misturar nomes de starters e imports de linhas diferentes do Boot. Registrar no primeiro PR as versões efetivamente adotadas, compatíveis entre si e com o ambiente da turma.

Para a demonstração local, usar H2 em arquivo, com caminho configurável. H2 em memória pode ser usado nos testes, mas não atende sozinho à persistência entre reinícios. Manter migrações versionadas com Flyway e validar o esquema, sem depender de `ddl-auto=update`. Se PostgreSQL for exigido, adotar esse banco desde a base e testar nele.

Separar controller, service, repository, entity e DTO. Controllers recebem e devolvem DTOs, nunca entidades JPA. Serviços concentram as regras e as transações. Preferir organização por funcionalidade:

```text
src/main/java/br/edu/uniesp/dearfuture/
├── DearFutureApplication.java
├── config/                 # CORS, Clock e exception/GlobalExceptionHandler
├── internal/auth/          # cadastro, login, User, repository e DTOs
├── internal/security/      # principal, JWT e respostas 401/403
├── internal/capsule/
│   ├── controller/          # Creation, Query e Opening controllers
│   ├── service/             # Creation, Query e Opening services
│   ├── entity/Capsule.java
│   ├── repository/CapsuleRepository.java
│   ├── dto/                 # records de entrada e saída
│   ├── mapper/CapsuleSummaryMapper.java
│   └── policy/CapsuleAvailability.java
└── shared/exception/        # exceções sem dependência de HTTP
src/main/resources/
├── application.yaml
├── application-local.yaml
└── db/migration/
src/test/java/br/edu/uniesp/dearfuture/
docs/
└── api.md
```

Usar `Clock` injetável para a regra temporal e testes determinísticos. A carta não exige tarefa agendada: a API compara o horário da requisição com `opensAt`.

## 4. Modelo e regras

O MVP passa a ter duas entidades: `User` e `Capsule`. Um usuário possui muitas cápsulas; cada cápsula tem exatamente um proprietário.

### Usuário

| Campo | Tipo proposto | Regra |
| --- | --- | --- |
| `id` | UUID | Gerado no servidor, usado como identidade no token |
| `name` | String | Obrigatório, 1 a 120 caracteres após trim |
| `email` | String | Obrigatório, formato válido, até 254 caracteres; normalizar com trim e minúsculas usando `Locale.ROOT` |
| `passwordHash` | String | Hash produzido por `PasswordEncoder`; nunca retornar em DTOs |
| `createdAt` | Instant | Gerado no servidor |

Criar restrição UNIQUE para o e-mail normalizado no banco. Tratar também concorrência de cadastro: a consulta prévia de existência não substitui a restrição. Retornar `409 EMAIL_ALREADY_REGISTERED` para duplicidade, sem detalhes de SQL. A divulgação de que um e-mail existe é uma escolha deste MVP local, a revisar antes de disponibilização pública.

Senha: de 12 a 128 caracteres, sem trim ou alteração silenciosa. Usar `PasswordEncoder` do Spring com função adaptativa unidirecional e parâmetros adequados ao ambiente. Proposta: `DelegatingPasswordEncoder` configurado para PBKDF2, com parâmetros explícitos e testados compatíveis com a versão usada. Nunca armazenar senha em texto puro, hash simples ou criptografia reversível. Não registrar senha, hash ou token em logs.

### Cápsula

Mapear o proprietário com `@ManyToOne(fetch = FetchType.LAZY, optional = false)` e `@JoinColumn(name = "owner_id", nullable = false)`, sem cascade de remoção para o usuário. Não é necessário mapear uma lista de cápsulas em `User`: consultar pelo repository evita navegação e serialização desnecessárias.


| Campo | Tipo proposto | Regra |
| --- | --- | --- |
| `id` | UUID | Gerado no servidor; enviado como string |
| `owner` | User / FK `owner_id` | Obrigatório; definido pela identidade autenticada |
| `title` | String | Obrigatório, remover espaços nas bordas, de 1 a 120 caracteres |
| `message` | String/texto | Obrigatório, remover espaços nas bordas, de 1 a 10.000 caracteres |
| `createdAt` | Instant | Gerado pelo servidor, imutável |
| `opensAt` | Instant | Obrigatório; estritamente posterior ao instante da criação |

Os limites de tamanho são propostas deste plano e devem ser usados também no formulário. O backend é a validação definitiva. Validar limites após normalizar os espaços, evitando critérios diferentes entre DTO e serviço.

- Receber datas ISO 8601 com `Z` ou offset explícito e devolver UTC com `Z`.
- O formulário converte a data/hora local para ISO antes de enviar; a API não interpreta uma data sem fuso como UTC silenciosamente.
- O servidor gera `id` e `createdAt` e define o proprietário a partir da autenticação. O request não aceita `ownerId` ou `userId`.
- Consultar sempre pelo proprietário: listagem por `owner.id`; detalhe e carta por `id` **e** `owner.id`. Para ID inexistente ou pertencente a outra conta, retornar o mesmo `404 CAPSULE_NOT_FOUND`.
- Na leitura, verificar a propriedade antes do horário: outro usuário recebe `404`, mesmo se a cápsula estiver disponível. Nunca carregar a coleção inteira para filtrar no frontend.
- No instante exato de `opensAt`, a carta já pode ser lida: `agora >= opensAt`.
- Não persistir um status que possa ficar desatualizado. Calcular `SEALED` ou `AVAILABLE` nas consultas usando o relógio do servidor.
- “Disponível” significa que a data chegou. Não haverá registro de primeira leitura nem estado `OPENED` no MVP.
- A listagem e o detalhe nunca devolvem `message`, mesmo depois da liberação. Somente o endpoint de leitura devolve a carta.
- A resposta de criação também não devolve `message`. A interface deixa de manter a carta no estado do formulário depois do sucesso.

## 5. Contrato HTTP a combinar antes do desenvolvimento paralelo

Base local proposta: `http://localhost:8080/api/v1`. Frontend: origem usada pelo Vite, inicialmente `http://localhost:5173`. Configurar as origens permitidas por ambiente; se a porta mudar, ajustar a configuração. Usar Bearer token no header `Authorization`, permitir esse header no preflight e expor `Location` se o cliente precisar lê-lo. Não usar cookies de autenticação nem `credentials: include` neste contrato.

### Autenticação e segurança

Escolha deste projeto: access token JWT de curta duração (15 minutos), enviado em `Authorization: Bearer <token>`. É uma decisão do DearFuture para a SPA e o trabalho paralelo, não um requisito extraído do exemplo do professor.

- Pessoa 1 implementa cadastro/login e integração com Spring Security. Login valida credenciais pelo mecanismo do Spring (`AuthenticationManager`/provider apropriado), sem comparar hashes manualmente.
- A emissão usa `JwtEncoder` e a validação usa o suporte OAuth2 Resource Server com `JwtDecoder`; evitar filtro JWT artesanal. O Resource Server valida tokens, mas não cria o endpoint de login.
- Fixar algoritmo de assinatura (proposta RS256), validar assinatura, expiração, emissor `dearfuture` e audiência `dearfuture-api`. `sub` contém apenas o UUID do usuário; não incluir senha, hash ou cartas. JWT assinado não é conteúdo criptografado.
- Chaves são geradas conforme instruções do README e carregadas de arquivos externos/configuração de ambiente. Não versionar chave privada, token real nem usar segredo fixo de exemplo em produção. Testes usam chaves próprias de teste.
- `SecurityFilterChain` libera somente cadastro/login e preflight CORS necessário; `/auth/me` e `/capsules/**` exigem autenticação; demais rotas são negadas por padrão. Sem form login, HTTP Basic ou sessão HTTP. Console H2 desligado por padrão.
- Política stateless: nenhuma autenticação por cookie. A desativação de CSRF fica restrita a esta API que autentica exclusivamente pelo Bearer header; se o contrato mudar para cookies, reavaliar a proteção antes da mudança.
- Sem token, token inválido ou expirado: `401`, JSON e header `WWW-Authenticate` adequado, sem redirecionamento para HTML. Falta de permissão de rota: `403`. Recurso de outro usuário: `404`.
- A Pessoa 5 fornece um escritor de `ProblemDetail` reutilizado pelo advice e pelos handlers de segurança (`AuthenticationEntryPoint` e `AccessDeniedHandler`). Exceções na cadeia de filtros não são cobertas automaticamente pelo advice MVC.
- No frontend, manter access token somente em memória. Recarregar a página exige login novamente. Logout remove token e todos os dados de usuário/cápsulas da memória; como não há revogação, um token previamente copiado continua válido até expirar. Não prometer logout global.
- Em `401` de uma consulta protegida, limpar autenticação, cancelar consultas pendentes e pedir novo login; não repetir automaticamente um `POST`. Ao trocar de conta, descartar também cache e respostas atrasadas da conta anterior.
- Demonstração em localhost; publicação futura exige HTTPS e medidas contra tentativas repetidas de login. Não descrever esta entrega acadêmica como pronta para operação pública.

### Endpoints

| Método e caminho | Resultado | Falhas previstas | Responsável |
| --- | --- | --- | --- |
| `POST /auth/register` | `201 Created`, `{id,name,email,createdAt}` sem hash ou token | `400` inválido; `409` e-mail duplicado | Pessoa 1 |
| `POST /auth/login` | `200 OK`, `{accessToken,tokenType:"Bearer",expiresIn:900}` | `400` inválido; `401` credenciais inválidas | Pessoa 1 |
| `GET /auth/me` | `200 OK`, `{id,name,email,createdAt}` | `401` não autenticado | Pessoa 1 |
| `POST /capsules` | `201 Created`, metadados e header `Location` do detalhe | `400` entrada inválida | Pessoa 2 |
| `GET /capsules` | `200 OK`, array de metadados; `[]` se vazio | `500` falha inesperada | Pessoa 3 |
| `GET /capsules/{id}` | `200 OK`, metadados | `400` UUID inválido; `404` inexistente | Pessoa 3 |
| `GET /capsules/{id}/message` | `200 OK`, carta liberada | `400` UUID inválido; `404` inexistente; `409` ainda selada | Pessoa 4 |

Todos os endpoints de cápsulas podem retornar `401`. `404` em detalhe/leitura também cobre recurso de outra conta. As respostas de autenticação usam `Cache-Control: no-store`.

Cadastro recebe `{name,email,password}` e login recebe `{email,password}`. Cadastro não autentica automaticamente: após sucesso, a pessoa realiza login. E-mail inexistente e senha incorreta geram a mesma mensagem de credenciais inválidas.

Listagem somente das cápsulas do usuário autenticado, sem paginação neste MVP local, ordenada por `createdAt` decrescente e `id` como desempate. Sem filtros no servidor nesta etapa; o frontend pode filtrar o array pelo status recebido.

### Requisição de criação

```json
{
  "title": "Uma carta para o futuro",
  "message": "Espero que você esteja bem!",
  "opensAt": "2027-01-01T12:00:00.000Z"
}
```

A data é apenas um exemplo: em testes manuais, sempre escolher um instante futuro em relação ao servidor.

### Metadados de criação e detalhe; itens da listagem

```json
{
  "id": "658bfb42-a1c8-4eb6-b742-ab4876dfbf76",
  "title": "Uma carta para o futuro",
  "createdAt": "2026-11-18T12:00:00.000Z",
  "opensAt": "2027-01-01T12:00:00.000Z",
  "status": "SEALED"
}
```

### Leitura liberada

```json
{
  "id": "658bfb42-a1c8-4eb6-b742-ab4876dfbf76",
  "message": "Espero que você esteja bem!"
}
```

Usar `Cache-Control: no-store` nas respostas da API de cápsulas para evitar reutilização de dados sensíveis ou do bloqueio temporal por caches HTTP. A integração deve verificar o comportamento real pelo navegador.

### Erros

Usar `ProblemDetail` em `@RestControllerAdvice`, seguindo o mecanismo do professor, com `Content-Type: application/problem+json`. O contrato anterior com `message` e `fieldErrors` foi substituído por `detail` e `erros`. O frontend deve tratar esse formato.

```json
{
  "type": "about:blank",
  "title": "Dados de requisição inválidos",
  "status": 400,
  "detail": "Revise os campos informados.",
  "instance": "/api/v1/capsules",
  "code": "VALIDATION_ERROR",
  "erros": {
    "opensAt": "Escolha uma data e hora futuras."
  }
}
```

Definir a extensão `erros` como objeto vazio quando não houver erros de campo. `code` também é uma extensão própria do contrato. Códigos: `VALIDATION_ERROR`, `INVALID_REQUEST`, `CAPSULE_NOT_FOUND`, `CAPSULE_SEALED`, `EMAIL_ALREADY_REGISTERED`, `INVALID_CREDENTIALS`, `UNAUTHENTICATED`, `ACCESS_DENIED` e `INTERNAL_ERROR`. Manter `409` para cápsula selada como decisão específica deste contrato; o exemplo do professor usa `422` para sua exceção genérica de negócio. Usar exceção específica para cápsula selada, sem mapear toda regra de negócio para `409`. JSON malformado, UUID inválido e data sem offset devem retornar `400`, sem stack trace. Erros inesperados retornam mensagem genérica, com detalhes somente no log do servidor. Não registrar o conteúdo das cartas nos logs.

O cliente pode exibir a contagem regressiva pelo relógio do dispositivo, mas a API decide a liberação. Se o botão estiver habilitado e o servidor responder `409`, preservar a tela, avisar que ainda não chegou o horário e permitir tentar novamente. Alterar o relógio do computador não deve liberar o conteúdo no servidor.

## 6. Divisão entre as mesmas cinco pessoas

A numeração acompanha o plano do frontend. Preencher nomes e logins antes de distribuir. Todos entregam código, testes e documentação de sua parte; documentação e integração não ficam como responsabilidade exclusiva da Pessoa 5.

| Pessoa | Continuidade com o frontend | Entrega principal no backend | Branch sugerida |
| --- | --- | --- | --- |
| 1 | Base, navegação e configuração | Base e autenticação (cadastro/login/segurança) | `<usuario>/base-autenticacao` |
| 2 | Criação | Criação de cápsulas e validação | `<usuario>/criacao-capsulas` |
| 3 | Persistência e listagem | Modelo, banco, consultas e listagem | `<usuario>/persistencia-consultas` |
| 4 | Detalhe e abertura | Regra temporal e leitura protegida | `<usuario>/abertura-capsulas` |
| 5 | Consumo de API e contexto | Contrato, erros, CORS/CI e preparação da integração | `<usuario>/contrato-integracao` |

### Pessoa 1 — base e autenticação

- [ ] Conferir o acesso das outras quatro pessoas ao repositório e preencher nomes/logins.
- [ ] Conferir a aplicação, Maven Wrapper e dependências recebidas na etapa zero; não recriar a base.
- [ ] Conferir H2 em arquivo, perfil de teste e Flyway já configurados na etapa zero.
- [ ] Reutilizar `Clock`, configuração JWT, PasswordEncoder, CORS e cadeia de segurança da etapa zero; validar o fluxo real de autenticação.
- [ ] Reutilizar User/UserRepository/migração V1 da base e implementar DTOs de cadastro/login.
- [ ] Implementar cadastro, `PasswordEncoder`, login, emissão JWT e `/auth/me`.
- [ ] Testar `SecurityFilterChain`, validação JWT e principal autenticado já entregues na base; não duplicar a configuração.
- [ ] Testar cadastro válido/duplicado, armazenamento de hash, credenciais inválidas, token adulterado/expirado e rotas protegidas.
- [ ] Atualizar `.gitignore` para banco local, artefatos de build e configurações privadas.
- [ ] Entregar a configuração inicial do build; Pessoa 5 configura CI e verifica CORS para equilibrar a carga.
- [ ] Documentar instalação, execução, portas e variáveis de ambiente no README.
- [ ] Verificar inicialização limpa e reinício com dados preservados junto à Pessoa 3.

**Aceite:** base executa; cadastro/login funcionam; credenciais não vazam; rotas privadas exigem token válido; nenhum segredo ou arquivo de banco está versionado.

### Pessoa 2 — criação

- [ ] Implementar `CreateCapsuleRequest`, validação e normalização das entradas.
- [ ] Implementar `CapsuleCreationService`, geração de UUID e `createdAt` e gravação transacional com o UUID do usuário autenticado.
- [ ] Receber identidade do principal no controller e passá-la explicitamente ao serviço; nunca aceitar o proprietário do corpo JSON.
- [ ] Implementar `CapsuleCreationController` com `POST /capsules`, `Location` e resposta sem a mensagem.
- [ ] Testar título/carta vazios, limites de tamanho, datas inválidas, datas passadas, igualdade com agora e criação válida.
- [ ] Preparar a adaptação do formulário para aguardar a API, desabilitar envio enquanto salva e manter campos em caso de erro.
- [ ] Documentar exemplos e passos de validação da criação.

**Aceite:** cria uma cápsula válida persistida; entrada inválida retorna `400` sem gravar; sucesso permite navegar ao detalhe pelo ID recebido.

### Pessoa 3 — modelo, persistência e consultas

- [ ] Reutilizar Capsule, CapsuleRepository e migração V2 entregues na etapa zero; validar as consultas e persistência.
- [ ] Implementar `CapsuleQueryController`, exclusivo dos endpoints de listagem e detalhe. Pessoas 2 e 4 usam controllers separados, sem editar este arquivo.
- [ ] Implementar `CapsuleQueryService` e o mapper de metadados que exclui a carta; usar o DTO comum entregue na base.
- [ ] Implementar listagem ordenada por proprietário e consulta por ID + proprietário; índice em `owner_id` e chave estrangeira para usuários.
- [ ] Testar com duas contas: listagem isolada e `404` ao buscar cápsula alheia.
- [ ] Testar persistência, ordenação, lista vazia e ID inexistente; conferir ausência de `message` nas respostas.
- [ ] Preparar a listagem e a Home para estados assíncronos e filtragem pelo novo status.
- [ ] Atualizar o diagrama de dados e validar persistência após reiniciar com a Pessoa 1.

**Aceite:** listagem e detalhe consultam o banco e não expõem a carta; dados sobrevivem ao reinício; migração funciona em banco vazio.

### Pessoa 4 — abertura e regra temporal

- [ ] Implementar `CapsuleOpeningService` e DTO da carta.
- [ ] Implementar `CapsuleOpeningController` com `GET /capsules/{id}/message` usando `Clock`.
- [ ] Usar `CapsuleAvailability` da base comum para o cálculo de disponibilidade; testar a regra sem depender do serviço de consultas da Pessoa 3.
- [ ] Testar antes, exatamente no horário e depois da abertura, além de ID inexistente.
- [ ] Testar requisição direta ao endpoint antes do prazo: resposta `409`, sem carta.
- [ ] Buscar por ID + proprietário; outra conta recebe `404` antes e depois do prazo. Testar sem token (`401`) e nunca revelar se a cápsula alheia está selada.
- [ ] Preparar o detalhe para carregar metadados e buscar a mensagem somente ao clicar em “Abrir cápsula”.
- [ ] Exibir carregamento/erro de leitura e tratar `409` mesmo quando o contador local terminou.

**Aceite:** alterar o relógio do navegador ou chamar a API diretamente não antecipa a abertura; após o horário a carta é exibida; falha dos feriados não interfere.

### Pessoa 5 — contrato, erros e integração

- [ ] Publicar `docs/api.md` com o contrato acordado antes dos endpoints.
- [ ] Reutilizar exceções e escritor da etapa zero e implementar o advice MVC global.
- [ ] Criar testes do formato dos erros, JSON inválido, UUID inválido e datas sem fuso.
- [ ] Validar CORS já conectado à cadeia de segurança; implementar CI (`./mvnw verify`), headers e testes de preflight em arquivos próprios.
- [ ] Reutilizar o escritor comum `ProblemResponses` da etapa zero no advice; handlers de `401`/`403` já usam esse contrato.
- [ ] Preparar no frontend o cliente HTTP autenticado, `authApi.ts`, `capsuleApi.ts`, configuração `VITE_API_BASE_URL` e tratamento de respostas não bem-sucedidas.
- [ ] Pessoa 1 implementa telas/contexto de cadastro/login/saída e proteção visual de rotas; Pessoa 5 oferece a base HTTP, sem concentrar nela todas as telas.
- [ ] Coordenar com Pessoas 2, 3 e 4 a substituição das chamadas síncronas, sem concentrar nela todas as alterações das telas.
- [ ] Manter a consulta de feriados independente, conforme o escopo atual do frontend.
- [ ] Consolidar exemplos de requisições e roteiro de demonstração com contribuições dos demais.

**Aceite:** o navegador consegue consumir a API; os erros têm formato único; cada tela consegue distinguir carregamento, sucesso, vazio e falha.

As alterações citadas no frontend são entregas futuras no outro repositório, em PRs próprios. Este documento não altera o frontend nem substitui automaticamente o trabalho já combinado para a disciplina de frontend.

## 7. Como liberar o trabalho paralelo

A dependência inevitável é uma **base comum inicial**. Depois dela, cada pessoa deve conseguir compilar, testar, fazer commits e abrir PRs sem aguardar as funcionalidades das outras. Não é necessário criar microsserviços, cinco projetos ou cinco bancos para isso.

### Etapa zero — contrato e base mínima

Você executará toda a preparação inicial usando o [guia da etapa zero](docs/roteiros/ETAPA-ZERO.md). Essa entrega concentra os arquivos compartilhados em um PR, com build, migrações e segurança funcionando. Os cinco integrantes não precisam entregar partes da base antes de começar suas funcionalidades.

| Entrega feita por você | Conteúdo |
| --- | --- |
| Projeto e ambiente | Java 21, Boot 4.1.1, Maven Wrapper 3.9.11, propriedades e Clock |
| Modelo persistente | User, Capsule com proprietário, repositories e migrações V1/V2 |
| Contratos comuns | DTO de metadados, mapper, política temporal e exceções |
| Segurança | PasswordEncoder, chaves externas, encoder/decoder JWT, CORS e cadeia de filtros |
| Erros compartilhados | Escritor de ProblemDetail para os filtros; advice MVC vem com a Pessoa 5 |
| Verificação | Teste da base com duas contas e persistência em banco de teste |

Após integrar a base, as cinco pessoas criam suas branches a partir da mesma `main`. As Pessoas 1 a 4 implementam seus casos de uso e endpoints; a Pessoa 5 implementa erros MVC, headers, contrato, CI e integração final. Não haverá endpoint de login fictício ou rota de cápsulas liberada provisoriamente.

**Ponto de partida comum:** base executável com usuário e cápsula completos, FK, repositories, DTO comum, política temporal e segurança. A implementação final de cadastro/login não precisa estar concluída para liberar os outros casos de uso: serviços recebem UUID explícito e testes criam seus próprios usuários.

Os arquivos de request específicos entram com o responsável pelo endpoint; não precisam ser escritos antes da rodada paralela. O mapper comum já estará implementado na etapa zero.

### Trabalho paralelo depois da base

| Pessoa | Arquivos principais exclusivos | Como testar sem esperar pelos colegas |
| --- | --- | --- |
| 1 | `internal/auth/`, `internal/security/`, `SecurityConfig`, README | Testes de autenticação com usuários de fixture; nenhum endpoint de cápsula necessário |
| 2 | `CapsuleCreationService`, `CapsuleCreationController` e testes de criação | Repository simulado nos testes de serviço; serviço simulado nos testes do controller; H2 nos testes de persistência |
| 3 | `CapsuleQueryService`, `CapsuleQueryController`, mapeamento de metadados e testes de consulta | Inserir cápsulas diretamente pelo repository nas fixtures, sem usar o endpoint de criação |
| 4 | `CapsuleOpeningService`, `CapsuleOpeningController`, DTO da carta e testes de abertura | Fixtures próprias no repository e `Clock` fixo; sem chamar criação ou consultas |
| 5 | Handler/escritor de erros, CORS, CI, `docs/api.md` e cliente HTTP futuro | Controllers exclusivos de teste que lançam as exceções acordadas; respostas HTTP simuladas para o cliente frontend |

Os três controllers podem usar a mesma base `/api/v1/capsules`, desde que cada um declare somente seus métodos/caminhos exclusivos. Isso evita três pessoas editando o mesmo arquivo.

As funcionalidades compartilham o repository e o contrato, mas **não chamam os serviços umas das outras**. A criação não chama o serviço de consultas para montar a resposta; usa o DTO comum. A abertura busca diretamente pelo repository e usa a regra temporal comum. Nenhum teste de consulta ou abertura precisa que alguém já tenha terminado o `POST`.

Para testar controllers antes do handler global ficar pronto, cada responsável valida os casos de sucesso com serviço simulado e os casos de negócio no teste do serviço. A Pessoa 5 valida a tradução das exceções para HTTP de forma isolada. A verificação completa dos status de erro nos endpoints reais ocorre na integração; não é motivo para bloquear commits anteriores.

### Autenticação sem bloquear as outras entregas

Fixar na base: `sub` do JWT e `Authentication.getName()` representam o UUID do usuário. Controllers convertem a identidade autenticada para UUID e passam `userId` explicitamente aos serviços; serviços não leem `SecurityContextHolder` nem dependem do login.

Pessoas 2, 3 e 4 testam serviços com UUIDs explícitos e usuários inseridos nas fixtures. Nos testes MVC, usam principal/JWT simulado pelo suporte de testes do Spring Security com o mesmo contrato. Testes de repository criam os próprios usuários sem chamar o cadastro. Não criar endpoint que aceite um ID arbitrário de usuário para facilitar teste manual.

A Pessoa 1 valida a assinatura real e a cadeia completa de segurança em testes próprios. Os testes MVC com principal simulado não comprovam a validação criptográfica do token. Essa combinação permite commits independentes e exige uma integração final real de login + token + cápsulas.

Se um PR funcional entrar antes da cadeia JWT definitiva, restringir o acesso até a segurança estar integrada; não publicar nem demonstrar uma versão com `permitAll` nas cápsulas. Não criar atalho de autenticação em produção.

### Commits e PRs independentes

Cada pessoa pode seguir esta sequência na própria parte:

1. Implementação da regra ou configuração com testes isolados.
2. Controller ou adaptação HTTP e testes correspondentes.
3. Exemplos de uso e documentação específica em `docs/pessoa-N.md`.
4. Ajustes encontrados na integração completa.

Commit não depende de merge alheio. Para integrar na `main`, o PR precisa compilar e passar nos testes disponíveis. Após a base, os PRs das Pessoas 2, 3, 4 e 5 podem ser revisados em qualquer ordem; os testes completos do navegador esperam a API integrada. Cada integrante atualiza sua branch antes do merge para conferir compatibilidade.

O mapper de metadados também entra na etapa zero feita por você: recebe a entidade e o instante capturado pelo serviço, usa a política temporal já acordada e retorna somente o DTO comum. Depois disso, criação e consultas reutilizam o mapper sem editar seu arquivo. O DTO da carta fica exclusivamente com a Pessoa 4.

### Arquivos compartilhados e revisão

- `pom.xml`, configuração geral e segurança/autenticação: Pessoa 1 coordena.
- CORS, CI e escritor de erros: Pessoa 5 coordena, em arquivos separados de `SecurityConfig`.
- Usuário, repository de usuário e migração V1: Pessoa 1. Cápsula, repository de cápsula e migração V2: Pessoa 3. Versões posteriores são combinadas para evitar colisões.
- DTO comum de metadados: definido na base; mudanças posteriores são combinadas com consumidores.
- Regra temporal comum: Pessoa 4 coordena alterações.
- Contrato consolidado e erros: Pessoa 5 coordena alterações. Os demais documentam exemplos em arquivos próprios e ela consolida.
- Controllers e testes de cada funcionalidade ficam em arquivos distintos.

Coordenação não significa exclusividade: outro integrante pode propor uma mudança por PR. Evitar alterações desnecessárias nos arquivos comuns durante a rodada paralela. Uma mudança de assinatura compartilhada deve ser integrada separadamente antes de começar a depender dela.

Migrações já integradas não devem ser reescritas. Cada mudança posterior recebe um novo arquivo com versão única combinada entre o grupo.

## 8. Integração futura com o frontend

A integração passa a incluir telas de cadastro/login, contexto de autenticação e saída. Proteger rotas no React melhora a navegação, mas a API deve aplicar toda a autorização.

Hoje `capsuleStorage.ts` retorna dados de forma síncrona e o tipo `Capsule` sempre exige `message`. Apenas trocar a URL não basta.

O contrato proposto passa a usar tipos separados:

```ts
type CapsuleSummary = {
  id: string
  title: string
  createdAt: string
  opensAt: string
  status: 'SEALED' | 'AVAILABLE'
}

type NewCapsule = {
  title: string
  message: string
  opensAt: string
}

type CapsuleMessage = { id: string; message: string }

// Assinaturas previstas para capsuleApi.ts
// listCapsules(signal?): Promise<CapsuleSummary[]>
// getCapsule(id, signal?): Promise<CapsuleSummary>
// createCapsule(input): Promise<CapsuleSummary>
// getCapsuleMessage(id, signal?): Promise<CapsuleMessage>
```

Preparação e distribuição da integração (os itens 2 a 4 podem ocorrer em paralelo):

1. Pessoa 5 entrega os tipos, cliente HTTP e exemplo de configuração em `.env.example`; todos revisam o contrato. Antes dos endpoints reais, ela pode testar o cliente com respostas HTTP simuladas. Depois dessa base do frontend, Pessoas 1 a 4 adaptam suas telas em paralelo usando o cliente e respostas simuladas, sem aguardar a API completa.
2. Pessoa 2 adapta formulário e navegação após criação com `await`.
3. Pessoa 3 adapta listagem; Pessoa 1 adapta o destaque da Home.
4. Pessoa 4 adapta detalhe, contador e busca da carta após ação do usuário.
5. Pessoa 1 entrega cadastro/login/contexto e saída; os demais usam o contrato de autenticação acordado. Todos verificam `401`, expiração, cancelamento de consultas e troca de conta sem reaproveitar dados anteriores.

O backend torna-se a fonte dos dados no modo integrado. Não usar fallback silencioso para localStorage quando a rede falhar: isso criaria duas coleções divergentes. Cápsulas antigas do navegador não aparecerão automaticamente no banco; a demonstração cria dados novos pela API. Se for necessário manter a entrega atual do frontend com localStorage, preservar essa versão por tag/branch e executar a integração em uma branch própria.

## 9. Marcos sugeridos

As datas de 29/11/2026 e das apresentações aparecem nos documentos do frontend; não há prazo de backend confirmado. Os marcos abaixo são relativos ao início aprovado pelo grupo, não datas de entrega acadêmica.

| Marco | Trabalho | Critério de saída |
| --- | --- | --- |
| 1 — base e contrato | Você: executar o roteiro completo da etapa zero | Base comum compilando e testada; funcionalidades liberadas para trabalho paralelo |
| 2 — funcionalidades | P1: cadastro/login/JWT; P2: criação com dono; P3: consultas isoladas; P4: abertura autorizada; P5: erros/CORS/CI/cliente HTTP | Endpoints e segurança funcionam e testes passam |
| 3 — integração | Cada pessoa adapta sua parte do frontend | Cadastrar → entrar → criar → listar → abrir → sair, com duas contas isoladas |
| 4 — entrega | Testes conjuntos, README, diagrama e roteiro | Clone limpo reproduz a demonstração; cinco contribuições revisadas |

Se apenas o backend fizer parte da entrega imediata, o marco 3 pode ocorrer depois. Mesmo assim, o contrato, CORS e testes HTTP devem estar prontos no marco 2.

## 10. Fluxo Git e revisão

- Cada pessoa usa sua identidade Git e abre PRs pequenos para `main`.
- Criar a branch a partir de `main` atualizada. Durante o desenvolvimento, executar `git fetch origin` e `git merge origin/main` na branch de trabalho.
- Não misturar frontend e backend no mesmo commit: são repositórios diferentes.
- Usar mensagens como `feat: criar capsulas com data futura` ou `test: validar bloqueio antes da abertura`.
- Usar mensagens de commit objetivas, descrevendo as alterações realizadas.
- Cada PR informa comportamento, contrato afetado e como validar; pelo menos outra pessoa revisa.
- Preservar os commits individuais com merge commit, para facilitar a avaliação de autoria.
- PR só é integrado se compilar e os testes relevantes passarem. Não deixar a verificação da aplicação para o último dia.
- Mudança de contrato exige atualizar `docs/api.md` e avisar as pessoas que consomem aquele endpoint.

## 11. Checklist de aceite do grupo

- [ ] Os cinco nomes/logins e requisitos da disciplina foram confirmados.
- [ ] Backend inicia seguindo o README em ambiente limpo.
- [ ] `./mvnw verify` passa e o CI valida os PRs.
- [ ] Criar, listar e consultar detalhe funciona com JSON e status HTTP acordados.
- [ ] Mensagens não aparecem nas respostas de criação, listagem e detalhe nem nos logs.
- [ ] Carta selada retorna `409` também quando a chamada é feita diretamente à API.
- [ ] Carta pode ser lida exatamente no instante de abertura e depois dele.
- [ ] Datas com fusos diferentes representam corretamente o mesmo instante.
- [ ] Campos inválidos, JSON inválido, UUID inválido e recurso inexistente têm erros previsíveis.
- [ ] Dados persistem após reiniciar a aplicação.
- [ ] Frontend recebe respostas da API pelo navegador, incluindo erros, sem bloqueio de CORS.
- [ ] Contador local não substitui a regra de liberação do servidor.
- [ ] Falha dos feriados não impede o fluxo de cápsulas.
- [ ] Cadastro/login/me funcionam e senhas são armazenadas exclusivamente como hash.
- [ ] Rotas protegidas retornam `401` sem token e com token inválido, adulterado ou expirado.
- [ ] Duas contas não compartilham listagem nem acessam detalhe/carta uma da outra, mesmo após a abertura.
- [ ] Proprietário é obtido do principal; não há `ownerId` aceito do cliente.
- [ ] Logout/troca de conta descartam token, dados e respostas pendentes; limitação de revogação documentada.
- [ ] `401` e `403` da cadeia de filtros seguem `ProblemDetail`; preflight continua funcionando.
- [ ] Chaves privadas, tokens, senhas e hashes não aparecem em logs ou commits.
- [ ] Diagramas representam o código real e cada integrante consegue demonstrar sua parte.

## 12. Decisões pendentes antes de implementar

1. Preencher os cinco nomes mantendo a correspondência com as responsabilidades do frontend.
2. Confirmar requisitos, stack e prazo específicos da disciplina de backend.
3. Confirmar com o professor apenas requisitos adicionais de segurança. Cadastro/login e autorização por dono já fazem parte do MVP por decisão do projeto.
4. Aprovar limites de campos, contrato HTTP e banco propostos neste documento.
5. Definir quando integrar o frontend sem comprometer a entrega atual baseada em localStorage.

## 13. Padrões das aulas incorporados ao projeto

### Fontes verificadas e limites da análise

- `Fundamentos de Back-End_01.pptx`: slides 3–6 (cliente, servidor e responsabilidades), 12–16 (HTTP, JSON e camadas), 18 (injeção por construtor) e 19 (autenticação e autorização).
- `Aula_01_BackEnd_Avancado_01.1.pptx`: slides 5–14 (responsabilidades, coesão, acoplamento, duplicação e SRP), 15–18 (organização e complexidade proporcional), 19 (DTOs), 22–25 (diagnóstico, justificativas e evolução).
- Repositório [assistencia-uniesp](https://github.com/JonasBernardino/assistencia-uniesp), consultado em 18/11/2026, revisão `ab273373fb4b5b2f49efb7b9fcc0a1740e26489e`. As observações abaixo se referem a essa revisão, não a versões futuras.

Os slides foram consultados pelo conteúdo textual dos arquivos. São referências de conceitos e exemplos, não um enunciado completo da avaliação do DearFuture. Os caminhos do repositório abaixo são evidências verificáveis dos padrões observados.

### Responsabilidades e convenções de implementação

| Padrão observado | Aplicação no DearFuture | Benefício e custo |
| --- | --- | --- |
| Controller fino, Service e Repository | Controller lida com HTTP e delega. Service coordena criação, consulta ou leitura. Repository acessa o banco | Testa regra sem HTTP; exige manter fronteiras claras |
| Injeção por construtor | Dependências `private final`, construtor explícito ou `@RequiredArgsConstructor`; sem injeção em campos | Dependências visíveis e serviços instanciáveis em testes |
| DTOs em `record` e `@Valid` | Request/response distintos da entidade; `@Valid @RequestBody`, `@NotBlank` para textos e validação de tamanho após normalização | Evita que esquema JPA exponha a carta; exige mapeamento |
| Mapper explícito | `CapsuleSummaryMapper` sem consultas ao banco, relógio próprio ou regras HTTP | Reutiliza conversão e impede divergência entre criação e consultas |
| Transações no Service | `@Transactional` na criação; `@Transactional(readOnly = true)` nas consultas e na leitura da carta | Unidade de trabalho clara; `readOnly` não substitui autorização |
| Entidade encapsulada | `@Entity`, tabela/colunas explícitas, construtor vazio `protected`, sem setters gerais | Impede alterações arbitrárias; não usar `@Data` indiscriminadamente em JPA |
| Erros centralizados | `@RestControllerAdvice`, `@ExceptionHandler` e `ProblemDetail` | Contrato uniforme; requer cobrir também falhas de conversão e JSON |
| Organização por funcionalidade | `internal/capsule` com subpacotes técnicos, além de `config` e `shared/exception` | Aproxima arquivos relacionados, sem alegar que isso por si só é DDD |
| Migrações e validação do esquema | Flyway + `ddl-auto=validate` e `open-in-view=false` | Banco reproduzível; migrações precisam corresponder ao mapeamento |

**Referências do código:** [controller](https://github.com/JonasBernardino/assistencia-uniesp/blob/ab273373fb4b5b2f49efb7b9fcc0a1740e26489e/src/main/java/br/edu/uniesp/assistencia/internal/cliente/controller/ClienteController.java), [service](https://github.com/JonasBernardino/assistencia-uniesp/blob/ab273373fb4b5b2f49efb7b9fcc0a1740e26489e/src/main/java/br/edu/uniesp/assistencia/internal/cliente/service/ClienteService.java), [DTO](https://github.com/JonasBernardino/assistencia-uniesp/blob/ab273373fb4b5b2f49efb7b9fcc0a1740e26489e/src/main/java/br/edu/uniesp/assistencia/internal/cliente/dto/CriarClienteRequest.java), [mapper](https://github.com/JonasBernardino/assistencia-uniesp/blob/ab273373fb4b5b2f49efb7b9fcc0a1740e26489e/src/main/java/br/edu/uniesp/assistencia/internal/cliente/mapper/ClienteMapper.java), [entidade](https://github.com/JonasBernardino/assistencia-uniesp/blob/ab273373fb4b5b2f49efb7b9fcc0a1740e26489e/src/main/java/br/edu/uniesp/assistencia/internal/cliente/entity/ClienteEntity.java), [handler](https://github.com/JonasBernardino/assistencia-uniesp/blob/ab273373fb4b5b2f49efb7b9fcc0a1740e26489e/src/main/java/br/edu/uniesp/assistencia/config/exception/GlobalExceptionHandler.java) e [configuração](https://github.com/JonasBernardino/assistencia-uniesp/blob/ab273373fb4b5b2f49efb7b9fcc0a1740e26489e/src/main/resources/application.yaml).

No DearFuture, o Service captura uma vez o instante de referência de cada operação e passa esse valor à política temporal e ao mapper. A entidade preserva seus dados válidos na criação; a política concentra a comparação temporal compartilhada. HTTP e `ResponseEntity` não entram na entidade ou no repository. Não duplicar a regra de abertura nos controllers.

`@Future` pode auxiliar uma validação, mas a decisão definitiva de data futura será feita com o `Clock` injetado no caso de uso, evitando dois relógios diferentes nos testes. Não adicionar a anotação sem configurar e testar a origem do relógio da validação.

### Adaptações deliberadas dos exemplos de aula

| Exemplo encontrado | Decisão para o DearFuture | Justificativa |
| --- | --- | --- |
| Perfil local com H2 em memória e `create-drop` | H2 em arquivo com Flyway e `validate`; memória somente em testes | A cápsula deve continuar existindo após reiniciar |
| Log de parâmetros JDBC em `TRACE` | Não ativar `org.hibernate.orm.jdbc.bind=TRACE`; evitar logs de corpos HTTP | Parâmetros de gravação podem conter a carta selada |
| Nome validado somente com `@NotNull` no DTO consultado | Texto obrigatório com `@NotBlank` e limites definidos | String vazia ou só espaços não é uma carta válida |
| Lista paginada com `Pageable` | Manter array no MVP local; planejar paginação se houver volume maior | Preserva o contrato simples já planejado; paginação exige mudar cliente e testes de forma coordenada |
| Identificador `Long` nas entidades de exemplo | Manter UUID como string na API | Compatível com o frontend; UUID não oferece autorização por si só |
| Um controller/service por entidade no exemplo | Manter arquivos por criação, consultas e abertura | Facilita PRs paralelos da equipe; são poucos casos de uso definidos, sem fragmentar cada método em uma classe |
| Perfil local ativado no arquivo geral | Selecionar perfil por ambiente/comando documentado | Evita carregar configuração local por acidente fora da demonstração |

JWT foi incluído por decisão do projeto para autenticar a SPA e manter a API stateless, com o custo de gerenciar chaves e expiração. Não adicionar automaticamente mensageria, Docker, interfaces de Service sem segunda implementação, microsserviços ou camadas de ports/adapters. Os slides orientam justificar complexidade pelo problema real. CORS e IDs difíceis de adivinhar não substituem autorização por proprietário.

### Testes e revisão alinhados à aula

- Regra temporal e casos de uso: testes unitários com `Clock` fixo e dependências simuladas, sem subir toda a aplicação.
- JPA e migrações: teste com banco vazio, mapeamentos e gravação/leitura; complementar com a verificação de reinício do banco em arquivo.
- Fronteira HTTP: testes de controller com serviço simulado, DTOs, códigos e JSON; handler validado separadamente e depois com os endpoints reais.
- Integração final: aplicação, banco e navegador, incluindo preflight de CORS. Responder no Postman não encerra a validação.

Cada PR relevante deve explicar **problema, consequência, proposta, benefício e custo**, conforme a atividade dos slides 22–23. Exemplo: “Retornar a entidade revelaria a carta na listagem. O DTO omite `message`, com custo de manter um mapper e seus testes.”

### Complemento das entregas individuais

- **Pessoa 1:** Java/Maven/Boot compatíveis, perfis, Flyway, usuário, cadastro/login e segurança JWT; logs sem cartas ou credenciais.
- **Pessoa 2:** request em `record`, validação de texto, transação de escrita e controller fino.
- **Pessoa 3:** entidade encapsulada, repository JPA, mapper comum entregue na base e transações de leitura.
- **Pessoa 4:** política temporal pura com instante recebido, uso de `Clock` no serviço e nenhum acoplamento ao protocolo HTTP na regra.
- **Pessoa 5:** `ProblemDetail`, advice e escritor comum para segurança, CORS/CI e cliente HTTP autenticado com tratamento de `detail`, `code` e `erros`.

Esses critérios complementam a divisão da seção 7 e preservam a autonomia dos commits após a base comum.

## 14. Roteiro de integração de autenticação e autorização

1. Cadastrar contas A e B com e-mails diferentes e realizar login em ambas.
2. A cria uma cápsula futura: o banco registra A como dono; corpo de criação não escolhe outro usuário.
3. Listagem de A contém a cápsula; listagem de B permanece vazia.
4. B consulta ID e carta de A diretamente: sempre `404`, sem título, mensagem ou informação do horário.
5. A consulta metadados antes do horário, mas recebe `409` ao pedir a carta.
6. Com relógio controlado em testes, no horário exato A recebe a carta; B continua recebendo `404`.
7. Sem token e com tokens adulterados/expirados, endpoints protegidos retornam `401` padronizado.
8. Login com senha errada e e-mail inexistente apresenta a mesma resposta; cadastro duplicado retorna `409` sem vazar SQL.
9. Sair de A e entrar como B no navegador não mantém cápsulas de A na tela, cache ou resultados de requisições atrasadas.
10. Reiniciar o backend preserva contas e cápsulas; chaves estáveis e configuração de validade mantêm o comportamento documentado.

### Referências técnicas complementares

A escolha de JWT e detalhes de armazenamento de senha complementam os conceitos das aulas; não foram atribuídos como exigências ao professor. Consultar a documentação correspondente à versão do Spring Security adotada na base:

- [Password Storage — Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html): `PasswordEncoder` e funções adaptativas de hash.
- [JWT Resource Server — Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html): validação de Bearer JWT e integração com o principal autenticado.

A entrega continua sendo um planejamento: nenhum endpoint ou mecanismo de login foi implementado por esta revisão do documento.

## 15. Sequência operacional dos commits

O [índice dos roteiros](docs/roteiros/README.md) define os arquivos e o código completo de cada etapa. São 12 commits sugeridos de implementação: um da base, dois de cada uma das Pessoas 1 a 4 e três da Pessoa 5. O terceiro da Pessoa 5 é o teste integrado e aguarda todos; os demais dependem somente da base e da sequência interna do próprio responsável.

A distribuição aqui descrita é por responsabilidade de manutenção. A primeira criação dos arquivos compartilhados fica toda com você. Os roteiros cobrem o backend; telas React e cliente HTTP futuro não entram nesses 12 commits e continuam como etapa posterior no outro repositório.
