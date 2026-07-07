package com.davi.poolbet.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Participante de um grupo. O saldo e liquidado na confianca entre os amigos do grupo;
 * o sistema apenas registra a contabilidade. O saldo e protegido por lock otimista
 * ({@link Version}) contra atualizacoes concorrentes quando o mesmo participante aposta
 * em mercados diferentes simultaneamente. O nome e unico dentro do grupo.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uq_users_grupo_nome", columnNames = { "grupo_id", "nome" }))
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "grupo_id", nullable = false)
	private Grupo grupo;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal saldo;

	@Version
	@Column(nullable = false)
	private Long version;

	protected User() {
		// JPA
	}

	public User(Grupo grupo, String nome, BigDecimal saldo) {
		this.grupo = grupo;
		this.nome = nome;
		this.saldo = saldo;
	}

	public Long getId() {
		return id;
	}

	public Grupo getGrupo() {
		return grupo;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	public void setSaldo(BigDecimal saldo) {
		this.saldo = saldo;
	}

	public Long getVersion() {
		return version;
	}
}
