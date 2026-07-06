# bolao-app

Prediction market com liquidação parimutuel (aposta mútua entre amigos, sem casa de apostas). Amigos criam mercados com pergunta binária, apostam num dos dois lados, e o criador resolve o resultado manualmente. O pool é rateado proporcionalmente entre os vencedores.

Projeto full-stack: Java Spring Boot + React + MySQL. Dinheiro fictício, sem integração externa, sem gateway de pagamento.

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
