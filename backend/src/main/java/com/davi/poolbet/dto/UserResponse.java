package com.davi.poolbet.dto;

import java.math.BigDecimal;

import com.davi.poolbet.model.User;

public record UserResponse(Long id, String nome, BigDecimal saldo) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getNome(), user.getSaldo());
	}
}
