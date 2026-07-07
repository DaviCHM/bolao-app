package com.davi.poolbet.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Sessao de login de um grupo. O token (aleatorio, 256 bits em hex) e enviado pelo
 * cliente no header {@code Authorization: Bearer <token>} e resolve o grupo autenticado.
 * Persistida em banco para sobreviver a restarts do servidor.
 */
@Entity
@Table(name = "sessoes")
public class Sessao {

	@Id
	@Column(length = 64)
	private String token;

	@ManyToOne(optional = false)
	@JoinColumn(name = "grupo_id", nullable = false)
	private Grupo grupo;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Sessao() {
		// JPA
	}

	public Sessao(String token, Grupo grupo) {
		this.token = token;
		this.grupo = grupo;
		this.criadoEm = Instant.now();
	}

	public String getToken() {
		return token;
	}

	public Grupo getGrupo() {
		return grupo;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
