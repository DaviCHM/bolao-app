package com.davi.poolbet.dto;

import com.davi.poolbet.model.Side;
import jakarta.validation.constraints.NotNull;

public record ResolveMarketRequest(
		@NotNull(message = "resultado (A ou B) e obrigatorio") Side resultado,
		@NotNull(message = "solicitanteId e obrigatorio") Long solicitanteId) {
}
