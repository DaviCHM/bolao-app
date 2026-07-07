package com.davi.poolbet.exception;

/** Falha de autenticacao: token ausente/invalido ou credenciais incorretas (HTTP 401). */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
