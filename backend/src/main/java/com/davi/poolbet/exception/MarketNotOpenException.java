package com.davi.poolbet.exception;

/**
 * Tentativa de apostar ou resolver/cancelar um mercado que nao esta ABERTO.
 *
 * <p>E a guarda de idempotencia critica do settlement: impede que um mercado ja
 * FECHADO seja resolvido de novo (o que creditaria payouts em dobro). Mapeada para
 * HTTP 409 (Conflict).</p>
 */
public class MarketNotOpenException extends RuntimeException {

	public MarketNotOpenException(String message) {
		super(message);
	}
}
