package com.davi.poolbet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMarketRequest(
		@NotBlank(message = "pergunta e obrigatoria") String pergunta,
		@NotBlank(message = "opcaoA e obrigatoria") String opcaoA,
		@NotBlank(message = "opcaoB e obrigatoria") String opcaoB,
		@NotNull(message = "criadorId e obrigatorio") Long criadorId) {
}
