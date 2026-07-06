package com.davi.poolbet.model;

/**
 * Ciclo de vida de um mercado.
 *
 * <ul>
 *   <li>{@code ABERTO} — aceita apostas; {@code resultado} nulo.</li>
 *   <li>{@code FECHADO} — resolvido com um lado vencedor ({@code resultado} = A ou B);
 *       payouts creditados.</li>
 *   <li>{@code CANCELADO} — anulado; todas as apostas reembolsadas; {@code resultado} nulo.</li>
 * </ul>
 */
public enum MarketStatus {
	ABERTO,
	FECHADO,
	CANCELADO
}
