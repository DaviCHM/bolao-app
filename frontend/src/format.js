// Formatacao pt-BR de dinheiro (ficticio), multiplicadores e probabilidades.
// Todos os numeros vem da API como decimais ja calculados — aqui e so exibicao.

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const num = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
const pct = new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 0 });

export const money = (v) => (v == null ? '-' : brl.format(v));

/** Multiplicador tipo "10x" / "1,11x"; "-" quando o lado esta vazio (odd nula). */
export const mult = (v) => (v == null ? '-' : `${num.format(v)}x`);

/** Probabilidade implicita tipo "90%"; "-" sem apostas. */
export const prob = (v) => (v == null ? '-' : pct.format(v));
