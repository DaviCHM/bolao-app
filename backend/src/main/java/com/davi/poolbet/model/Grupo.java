package com.davi.poolbet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Grupo de amigos: a unidade de login do sistema. Cada grupo tem nome unico e senha
 * compartilhada; participantes ({@link User}) e mercados pertencem a exatamente um
 * grupo e nunca sao visiveis fora dele.
 */
@Entity
@Table(name = "grupos")
public class Grupo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String nome;

	/** Hash BCrypt da senha compartilhada do grupo. Nunca exposto pela API. */
	@Column(name = "senha_hash", nullable = false, length = 100)
	private String senhaHash;

	protected Grupo() {
		// JPA
	}

	public Grupo(String nome, String senhaHash) {
		this.nome = nome;
		this.senhaHash = senhaHash;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getSenhaHash() {
		return senhaHash;
	}
}
