package com.davi.poolbet.exception;

/**
 * Usuario tentou apostar mais do que tem de saldo. Mapeada para HTTP 422 (Unprocessable
 * Entity): a requisicao e valida sintaticamente, mas viola uma regra de negocio.
 */
public class InsufficientBalanceException extends RuntimeException {

	public InsufficientBalanceException(String message) {
		super(message);
	}
}
