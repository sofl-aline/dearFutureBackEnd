# Regras de negócio — DearFuture

## Escopo e entidades

DearFuture permite guardar cartas privadas para leitura futura. O modelo tem duas entidades com identidade própria: `User` (conta proprietária) e `Capsule` (carta selada com data de liberação). A carta é um atributo da cápsula: não possui ciclo de vida independente. Título, e-mail e datas são valores. Resumos e tokens são dados de transporte, não entidades.

Um usuário possui de zero a muitas cápsulas. Cada cápsula possui exatamente um usuário. O mapeamento JPA é unidirecional, de `Capsule.owner` para `User`, sem remoção em cascata. A ausência de uma coleção em `User` não altera a multiplicidade.

Anexos, destinatários externos, compartilhamento, notificações, edição/exclusão de cápsulas e recuperação de senha permanecem fora do MVP. Só devem originar novas entidades quando houver requisitos aprovados que exijam identidade e persistência próprias.

## Catálogo de regras

| ID | Regra verificável | Responsabilidade e situação |
| --- | --- | --- |
| RN01 | Usuário tem identidade UUID e data de criação obrigatórias; ambos permanecem estáveis. | Entidade `User`; implementada. O futuro caso de uso fornece UUID e horário do servidor. |
| RN02 | Nome é obrigatório e contém de 1 a 120 caracteres após remover espaços nas bordas. Alterar o nome não altera identidade, e-mail, senha ou cápsulas. | Construtor e `User.changeDisplayName`; implementada. |
| RN03 | E-mail é obrigatório, tem formato de endereço, até 254 caracteres e é armazenado sem espaços nas bordas e em minúsculas com `Locale.ROOT`. Não há alteração de e-mail no MVP. | Construtor de `User`; implementada. Validação de formato não comprova existência da caixa postal. |
| RN04 | Não podem existir duas contas com o mesmo e-mail normalizado. | Restrição UNIQUE no banco; implementada. O futuro cadastro traduzirá a duplicidade para erro de negócio. |
| RN05 | Senha informada tem de 12 a 128 caracteres e não é normalizada. Só seu hash adaptativo pode ser persistido; não é retornado ao cliente. | Encoder configurado. Validação da senha e geração do hash cabem ao futuro cadastro. `User` exige hash não vazio de até 255 caracteres; não recebe senha em texto puro. |
| RN06 | Cápsula tem UUID, proprietário e instante de criação obrigatórios e estáveis. O cliente não escolhe outro proprietário. | Entidade e FK implementadas; vínculo com a identidade autenticada cabe ao futuro caso de uso. |
| RN07 | Título e carta são obrigatórios, normalizados com trim e limitados respectivamente a 120 e 10.000 caracteres. | Construtor de `Capsule`; implementada. |
| RN08 | Data de liberação é obrigatória e estritamente posterior ao instante de criação. | Construtor e CHECK no banco; implementada. O futuro caso de uso fornece o horário do servidor. |
| RN09 | Após a criação, título, carta, proprietário e datas da cápsula não podem ser alterados. | Ausência de métodos de alteração na entidade; implementada. |
| RN10 | Carta está disponível se e somente se agora for maior ou igual a `opensAt`. Consultar não registra leitura nem modifica a cápsula. | `CapsuleAvailability.isAvailable` e mapper; implementada. Futuras consultas fornecem horário por `Clock`. |
| RN11 | Somente o proprietário pode consultar ou ler sua cápsula. Para outra conta, o resultado deve equivaler a cápsula inexistente. | Repository já oferece busca por proprietário. Autorização e resposta HTTP serão implementadas nos casos de uso. |
| RN12 | Antes da liberação, nem o proprietário pode ler a carta. Listagens, detalhes e resposta de criação mostram somente metadados, mesmo após a liberação. | DTO de resumo implementado. Restrição de leitura e endpoints pertencem às próximas etapas. |

## Comportamentos e invariantes

O modificador `changeDisplayName(String)` representa a alteração permitida no perfil. Valida a entrada antes da atribuição; uma tentativa inválida preserva o nome anterior. Não há setters genéricos. A alteração do perfil não exige endpoint nesta entrega.

Os construtores estabelecem os estados iniciais válidos. Construtores vazios protegidos existem exclusivamente para JPA. Não se adiciona um método de edição de cápsula apenas para cumprir uma quantidade de métodos: isso contrariaria RN09. Não se persiste `SEALED`/`AVAILABLE`, pois o tempo pode mudar sem gravação no banco.

Validações HTTP futuras melhoram mensagens ao usuário, mas não substituem invariantes nas entidades. Regras entre registros, como unicidade, precisam também de restrições no banco para suportar concorrência. Entidades não consultam repositories, relógio global ou autenticação.

## Exemplos de aceite

- Nome ` Ana ` é armazenado como `Ana`; alterar para branco falha e preserva o nome anterior.
- E-mail ` ANA@EXAMPLE.COM ` é armazenado como `ana@example.com`; cadastrar novamente o mesmo endereço falha pela unicidade.
- Criação com `opensAt` igual ou anterior a `createdAt` falha.
- Um instante antes da liberação resulta em `SEALED`; no instante exato resulta em `AVAILABLE`.
- Consulta da cápsula de outra conta não encontra resultado, mesmo após o horário de abertura.

## Relação com os roteiros

Os roteiros das Pessoas 1 a 5 continuam cobrindo as funcionalidades futuras. O código em `src/` e estas regras representam o domínio atual. Não substituir entidades por versões antigas copiadas de exemplos. Nenhum Controller ou Service funcional é necessário para esta entrega acadêmica.
