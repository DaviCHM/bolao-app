package com.davi.poolbet.model;

/**
 * Lado de um mercado binario. Usado tanto para o lado escolhido numa aposta
 * ({@link Bet#getOpcao()}) quanto para o lado vencedor de um mercado resolvido
 * ({@link Market#getResultado()}).
 */
public enum Side {
	A,
	B
}
