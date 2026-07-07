package com.davi.poolbet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciais de registro/login de um grupo. */
public record AuthRequest(
		@NotBlank @Size(max = 100) String nome,
		@NotBlank @Size(min = 4, max = 100) String senha) {
}
