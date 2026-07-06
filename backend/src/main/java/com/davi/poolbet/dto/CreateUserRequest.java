package com.davi.poolbet.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
		@NotBlank(message = "nome e obrigatorio") String nome) {
}
