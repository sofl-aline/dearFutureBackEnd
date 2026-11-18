# Roteiros de implementação — DearFuture Backend

Estes arquivos detalham o planejamento em etapas executáveis, com caminhos, código completo, testes e mensagens de commit. A etapa zero será feita por você; depois dela, cada uma das cinco pessoas pode desenvolver sua parte em paralelo.

A etapa zero já está implementada em `src/` e publicada no repositório. Os blocos dos guias descrevem as funcionalidades seguintes; criar somente os arquivos da sua etapa e preservar a base existente.

Consultar também as [regras de negócio atuais](../REGRAS-DE-NEGOCIO.md) e preservar as entidades existentes ao seguir os roteiros. A alteração de nome é um comportamento do domínio; não exige endpoint nesta entrega.

## Ordem e divisão

| Responsável | Guia | Commits de implementação |
| --- | --- | --- |
| Você | [Etapa zero](ETAPA-ZERO.md) | 1 — base comum completa |
| Pessoa 1 | [Autenticação](PESSOA-1-AUTENTICACAO.md) | 2 — serviço de cadastro/login; endpoints e token real |
| Pessoa 2 | [Criação](PESSOA-2-CRIACAO.md) | 2 — caso de uso; endpoint |
| Pessoa 3 | [Consultas](PESSOA-3-CONSULTAS.md) | 2 — serviço/banco; endpoints |
| Pessoa 4 | [Abertura](PESSOA-4-ABERTURA.md) | 2 — autorização/horário; endpoint da carta |
| Pessoa 5 | [Integração](PESSOA-5-INTEGRACAO.md) | 3 — erros/headers; contrato/CI; teste integrado final |

São 12 commits sugeridos de implementação, incluindo a base. Cada integrante pode subdividir um commit quando houver uma mudança coerente e testável; quantidade de commits não substitui contribuição real. PRs e commits de merge não entram nessa contagem.

Os primeiros dois commits de cada pessoa dependem somente da etapa zero e dos próprios commits anteriores. **Somente o último commit da Pessoa 5 aguarda os endpoints de todos**, para executar o fluxo integrado. Não esperar o POST de criação para testar consultas/abertura: usar as fixtures dos roteiros.

## Contratos já definidos na base

- Java 21, Spring Boot 4.1.1, Maven Wrapper 3.9.11, JPA, Flyway, H2 e Spring Security.
- Usuário e cápsula com proprietário; repositories, DTO comum, mapper e política de abertura.
- Clock injetável; services recebem UUID autenticado sem ler SecurityContextHolder.
- JWT RS256 com emissor/audiência e expiração; chaves externas geradas por clone.
- Segurança/CORS e escritor comum de erros, sem endpoint de autenticação provisório.
- Os controllers de criação, consulta e abertura estão em arquivos diferentes.

Essa distribuição coloca os arquivos compartilhados na etapa zero para reduzir a espera e os conflitos. Os roteiros operacionais refinam a antiga distribuição da etapa zero no planejamento: você entrega a infraestrutura compartilhada; os integrantes entregam os casos de uso e testes indicados aqui.

## Como usar os códigos

1. A partir da raiz do backend, criar cada arquivo com o conteúdo completo indicado no bloco do guia.
2. Respeitar a sequência dos commits dentro da sua parte.
3. Gerar as chaves locais uma vez, executar testes e revisar o diff antes de fazer commit.
4. Abrir PRs pequenos, com autoria individual e revisão de outra pessoa. Preservar os commits no merge.
5. Integrar funcionalidades em qualquer ordem após a base. Sincronizar com origin/main antes de concluir o PR.
6. Depois de todos os PRs, executar o último teste da Pessoa 5 e conferir o fluxo no navegador durante a integração futura do frontend.

`README.md` e `docs/api.md` aparecem como blocos de conteúdo completo nos guias. Não confundir os arquivos de implementação com estes documentos: adicionar ao commit somente os caminhos listados em cada etapa. A publicação dos roteiros pode ser feita em um commit de documentação separado, se o grupo quiser versioná-los.

## Limites e decisões

Este pacote implementa o backend do MVP com cadastro/login. Não inclui telas novas de login, cliente HTTP React, recuperação de senha, refresh token, notificações ou upload. A adaptação do frontend permanece planejada no documento principal e deverá ocorrer em seu repositório; a API já tem contrato para ela.

O banco não criptografa as cartas em repouso nesta versão. O bloqueio é feito pela API; administrador do banco pode ler os dados. Utilizar dados fictícios na demonstração.

Tokens ficam somente em memória no futuro cliente; recarregar exige novo login. Não há revogação imediata. A demonstração local não equivale a uma implantação pública pronta.

## Referências

- [Planejamento geral](../../PLANEJAMENTO-BACKEND.md), com a análise dos slides e do código do professor.
- [Spring Boot — testes](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html).
- [Spring Security — senhas](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
- [Spring Security — JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## Validação técnica dos exemplos

Os exemplos Java foram montados fora do repositório e verificados com Java 21, Boot 4.1.1 e Maven 3.9.11. A base e os dois commits de cada pessoa foram compilados/testados independentemente das funcionalidades dos outros integrantes. O projeto integrado passou em 26 testes, incluindo JWT real, bloqueio temporal e duas contas.

O Maven Wrapper oficial foi gerado pelo comando indicado na etapa zero. Não foram executados os commits ou o workflow no GitHub, nem modificadas telas React. Os testes HTTP usam MockMvc; não substituem a futura verificação visual e de rede no navegador.
