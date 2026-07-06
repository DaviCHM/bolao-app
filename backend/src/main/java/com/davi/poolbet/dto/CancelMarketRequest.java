package com.davi.poolbet.dto;

import jakarta.validation.constraints.NotNull;

public record CancelMarketRequest(
		@NotNull(message = "solicitanteId e obrigatorio") Long solicitanteId) {
}
