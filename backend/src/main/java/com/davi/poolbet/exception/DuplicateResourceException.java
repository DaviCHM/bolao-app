package com.davi.poolbet.exception;

/**
 * Violacao de unicidade de dominio (ex.: nome de usuario ja existente).
 * Mapeada para HTTP 409 (Conflict).
 */
public class DuplicateResourceException extends RuntimeException {

	public DuplicateResourceException(String message) {
		super(message);
	}
}
