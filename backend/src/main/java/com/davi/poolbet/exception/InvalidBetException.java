package com.davi.poolbet.exception;

/**
 * Aposta invalida por regra de dominio (ex.: valor menor ou igual a zero, ou com mais
 * de duas casas decimais). Mapeada para HTTP 400 (Bad Request).
 */
public class InvalidBetException extends RuntimeException {

	public InvalidBetException(String message) {
		super(message);
	}
}
