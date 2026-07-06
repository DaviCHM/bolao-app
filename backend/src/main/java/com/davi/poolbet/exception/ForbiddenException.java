package com.davi.poolbet.exception;

/**
 * Acao nao permitida para o solicitante (ex.: resolver/cancelar um mercado que nao criou).
 * Mapeada para HTTP 403 (Forbidden).
 */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}
}
