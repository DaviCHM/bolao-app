package com.davi.poolbet.exception;

/**
 * Recurso referenciado (usuario, mercado, aposta) nao existe. Mapeada para HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
