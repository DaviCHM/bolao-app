<p align="center">
  <img src="frontend/public/logo.png" alt="bolao-app" width="96" />
</p>

# bolao-app

Bolão parimutuel entre amigos: sem casa de apostas, sem odds fixas — o pool inteiro é
rateado entre quem acertou.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?style=flat&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?style=flat&logo=vite&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=flat&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-multi--stage-2496ED?style=flat&logo=docker&logoColor=white)

**Demo ao vivo:** https://bolao-app-2x3i.onrender.com
(instância gratuita — a primeira visita pode levar cerca de 1 minuto para o serviço acordar)

## Como funciona

Um grupo de amigos compartilha um login (nome + senha). Dentro do grupo, qualquer um cria
um mercado com uma pergunta binária ("o time X vence?", opção A ou B), os participantes
apostam num dos dois lados, e o criador resolve o resultado manualmente. Não há odds fixas
nem casa cobrando margem: o pool inteiro (soma de tudo que foi apostado nos dois lados) é
dividido proporcionalmente entre quem apostou no lado vencedor. A liquidação em dinheiro
real acontece na confiança entre os amigos, fora do app — o sistema só registra saldo,
apostas e o rateio.

Exemplo: pool de R$ 300 (R$ 150 no lado vencedor, R$ 150 no lado perdedor). Cada aposta do
lado vencedor recebe o dobro do que apostou, porque `payout = valor_apostado / total_vencedor
* pool`. Quem apostou R$ 30 recebe R$ 60; quem apostou R$ 10 recebe R$ 20.

## Stack

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 17, Spring Boot 4.1 (Web MVC, Data JPA, Validation), MySQL + Flyway, BCrypt (`spring-security-crypto`), JUnit/Mockito/MockMvc com H2 |
| Frontend | React 19, Vite 7, CSS puro com design tokens (tema claro/escuro), Phosphor Icons, sem lib de roteamento (hash routing) |
| Infra | Dockerfile multi-stage (frontend embutido no jar), docker-compose com MySQL |

## Destaques técnicos

- Settlement parimutuel com o método *largest remainder* para distribuir o residuo de
  centavos, com invariante testada (`soma(payouts) == pool`) em
  `SettlementCalculator` — função pura, sem Spring nem banco.
- Concorrência tratada em duas camadas: lock pessimista (`PESSIMISTIC_WRITE`) no
  `Market` para serializar apostas e resolução, e lock otimista (`@Version`) no `User`
  para apostas simultâneas em mercados diferentes; conflito retorna `409`.
- Multi-tenancy por grupo: sessão via header `Authorization: Bearer <token>` com TTL
  configurável (`BOLAO_SESSAO_TTL_DIAS`); acesso a recurso de outro grupo responde `404`,
  não `403`, para não vazar existência.
- Migrações versionadas com Flyway (`V1__init.sql`, `V2__grupos.sql`), sem alteração de
  schema fora de migration.
- Dinheiro sempre `BigDecimal` em Java e `DECIMAL(19,2)` no banco — nunca `double`.
- Tratamento de erro centralizado num `@ControllerAdvice` global, com mapeamento
  consistente de exceção de domínio para status HTTP.

## Rodando local

### Com Docker

O `Dockerfile` na raiz builda tudo em uma imagem só (frontend compilado + backend), e o
`docker-compose.yml` sobe app + MySQL:

1. Copie `.env.example` para `.env` e preencha `MYSQL_USER`, `MYSQL_PASSWORD` e
   `MYSQL_ROOT_PASSWORD` (o `.env` fica fora do git).

2. Suba os serviços:

   ```bash
   docker compose up --build
   ```

   Abra http://localhost:8080. O banco usa volume nomeado, então os dados sobrevivem a
   `docker compose down` (some apenas com `down -v`).

### Deploy

O `Dockerfile` é auto-suficiente: qualquer plataforma que builde um Dockerfile consegue
publicar o app sem passo extra de build. Basta injetar as variáveis de ambiente do banco:

- `SPRING_DATASOURCE_URL` — para banco gerenciado com TLS, use o formato
  `jdbc:mysql://<host>:<porta>/<database>?sslMode=REQUIRED`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

A porta é lida da variável `PORT` automaticamente (fallback 8080). As sessões expiram
após 30 dias (configurável via `BOLAO_SESSAO_TTL_DIAS`).

### Sem Docker (desenvolvimento)

1. **Backend** (precisa de MySQL local; credenciais via `MYSQL_USER`/`MYSQL_PASSWORD`):

   ```bash
   cd backend && ./mvnw spring-boot:run
   ```

2. **Frontend** (proxy do Vite encaminha `/api` ao backend na porta 8080):

   ```bash
   cd frontend && npm install && npm run dev
   ```

   Abra http://localhost:5173. Detalhes em [frontend/README.md](frontend/README.md).

## API

Base: `http://localhost:8080/api`. Fora de `/auth`, toda rota exige o header
`Authorization: Bearer <token>` (token de sessão do grupo). Recursos de outro grupo
respondem `404`.

| Método | Rota | Descrição |
| --- | --- | --- |
| POST | `/auth/register` | Cria um grupo `{nome, senha}` e abre sessão → `{token, grupoId, grupoNome}` |
| POST | `/auth/login` | Autentica o grupo e abre sessão |
| POST | `/auth/logout` | Encerra a sessão do token enviado |
| POST | `/users` | Cria participante `{nome}` no grupo (saldo inicial 1000.00) |
| GET | `/users` | Lista participantes do grupo |
| GET | `/users/{id}` | Detalha um participante |
| POST | `/markets` | Cria mercado `{pergunta, opcaoA, opcaoB, criadorId}` |
| GET | `/markets?status=` | Lista mercados do grupo (filtro opcional: ABERTO/FECHADO/CANCELADO) |
| GET | `/markets/{id}` | Detalhe com apostas e métricas derivadas (pool, odds, probabilidades) |
| POST | `/markets/{id}/bets` | Aposta `{usuarioId, opcao: "A"\|"B", valor}` |
| POST | `/markets/{id}/resolve` | Resolve `{resultado, solicitanteId}` — só o criador |
| POST | `/markets/{id}/cancel` | Cancela e reembolsa `{solicitanteId}` — só o criador |

Erros: `400` validação, `401` sem sessão/credencial inválida, `403` não é o criador,
`404` não encontrado (ou de outro grupo), `409` conflito (mercado fechado, nome duplicado,
concorrência retryável), `422` saldo insuficiente.

## Testes

```bash
cd backend && ./mvnw test
```

Rodam em H2 em memória, sem precisar de MySQL. Total de 38 testes cobrindo:

- `SettlementCalculator` — regra parimutuel, largest remainder e casos de borda (lado
  vencedor vazio, lado perdedor vazio, mercado sem apostas).
- Serviços (`AuthService`, `BetService`, `MarketService`) — validações de domínio,
  isolamento por grupo e regras de saldo.
- `BettingFlowIntegrationTest` — fluxo completo via MockMvc (registro de grupo, criação
  de usuário e mercado, apostas, resolução).
- `GlobalExceptionHandlerTest` — mapeamento de exceção de domínio para status HTTP.

## Estrutura do repo

```
backend/
  src/main/java/com/davi/poolbet/
    controller/   endpoints REST (Auth, Market, User)
    service/      regras de negócio e SettlementCalculator
    model/        entidades JPA (Grupo, User, Market, Bet, Sessao)
    repository/   Spring Data JPA, incluindo o lock pessimista do Market
    dto/          contratos de request/response
    exception/    exceções de domínio e o ControllerAdvice global
    config/       AuthInterceptor e configuração de sessão
  src/main/resources/db/migration/   migrations Flyway (V1, V2)
  src/test/java/    testes de unidade, serviço e integração (MockMvc + H2)
frontend/
  src/components/   telas e componentes React (Home, Login, Market, apostas)
  src/api.js        client HTTP da API
  src/styles.css    design tokens e temas claro/escuro
Dockerfile            build multi-stage (frontend + backend numa imagem)
docker-compose.yml    app + MySQL para desenvolvimento/produção simples
```
