# bolao-app

Prediction market com liquidação parimutuel (aposta mútua entre amigos, sem casa de apostas). Cada grupo de amigos tem seu login; dentro dele criam mercados com pergunta binária, apostam num dos dois lados, e o criador resolve o resultado manualmente. O pool é rateado proporcionalmente entre os vencedores.

Projeto full-stack: Java Spring Boot + React + MySQL. A liquidação é feita na confiança entre os amigos do grupo: sem integração externa, sem gateway de pagamento.

## Como rodar

1. **Backend** (precisa de MySQL local; credenciais via `MYSQL_USER`/`MYSQL_PASSWORD`):

   ```bash
   cd backend && ./mvnw spring-boot:run
   ```

   Testes (H2 em memória, não precisa de MySQL): `./mvnw test`

2. **Frontend** (proxy do Vite encaminha `/api` ao backend na porta 8080):

   ```bash
   cd frontend && npm install && npm run dev
   ```

   Abra http://localhost:5173. Detalhes em [frontend/README.md](frontend/README.md).

## Rodando com Docker

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
