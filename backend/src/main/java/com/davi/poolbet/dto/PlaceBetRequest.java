package com.davi.poolbet.dto;

import java.math.BigDecimal;

import com.davi.poolbet.model.Side;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceBetRequest(
		@NotNull(message = "usuarioId e obrigatorio") Long usuarioId,
		@NotNull(message = "opcao (A ou B) e obrigatoria") Side opcao,
		@NotNull(message = "valor e obrigatorio")
		@Positive(message = "valor deve ser maior que zero") BigDecimal valor) {
}
