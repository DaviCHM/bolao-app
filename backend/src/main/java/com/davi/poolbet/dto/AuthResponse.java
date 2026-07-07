package com.davi.poolbet.dto;

import com.davi.poolbet.model.Sessao;

/** Sessao aberta: o cliente guarda o token e o envia como {@code Authorization: Bearer}. */
public record AuthResponse(String token, Long grupoId, String grupoNome) {

	public static AuthResponse from(Sessao sessao) {
		return new AuthResponse(sessao.getToken(), sessao.getGrupo().getId(), sessao.getGrupo().getNome());
	}
}
