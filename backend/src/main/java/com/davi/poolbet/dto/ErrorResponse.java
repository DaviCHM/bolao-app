package com.davi.poolbet.dto;

import java.time.OffsetDateTime;

/**
 * Corpo de erro padronizado retornado pelo handler global.
 */
public record ErrorResponse(
		OffsetDateTime timestamp,
		int status,
		String error,
		String message) {

	public static ErrorResponse of(int status, String error, String message) {
		return new ErrorResponse(OffsetDateTime.now(), status, error, message);
	}
}
