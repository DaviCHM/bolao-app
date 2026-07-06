# Frontend — Bolão

Interface web do sistema de apostas mútuas (parimutuel) entre amigos.
React + Vite, CSS puro (variáveis, grid/flex, dark mode). Sem framework de UI.

## Rodando em desenvolvimento

Pré-requisito: backend no ar em `http://localhost:8080` (ver `../backend`).

```bash
npm install
npm run dev
```

Abra http://localhost:5173. O proxy do Vite encaminha `/api` para o backend —
não há configuração de CORS.

## Como funciona

- **Usuário atual**: não há login; escolha "quem você é" no topo (persistido em
  `localStorage`). Cada usuário novo nasce com saldo fictício de R$ 1.000.
- **Lista de mercados**: cards com a barra de proporção do pool (verde = lado A,
  roxo = lado B), probabilidade implícita e multiplicador por lado. O lado com
  menos dinheiro paga mais — o chip "PAGA Nx" marca o azarão.
- **Detalhe**: apostar em um lado (com estimativa de retorno já considerando o
  efeito da própria aposta no pool), e — apenas para o criador — resolver A/B ou
  cancelar (reembolsa todos).
- **Resultado**: lado vencedor em destaque e tabela de payouts; o rodapé evidencia
  a invariante do parimutuel: soma dos payouts = pool, ao centavo.

Todos os números (odds, probabilidades, payouts) vêm calculados do backend; o
frontend apenas exibe — exceto a estimativa de retorno pré-aposta, que é local.
