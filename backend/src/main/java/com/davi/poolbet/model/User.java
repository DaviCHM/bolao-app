package com.davi.poolbet.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Participante do sistema. Possui um saldo em "dinheiro ficticio" (nao ha dinheiro real).
 * O saldo e protegido por lock otimista ({@link Version}) contra atualizacoes concorrentes
 * quando o mesmo usuario aposta em mercados diferentes simultaneamente.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String nome;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal saldo;

	@Version
	@Column(nullable = false)
	private Long version;

	protected User() {
		// JPA
	}

	public User(String nome, BigDecimal saldo) {
		this.nome = nome;
		this.saldo = saldo;
	}

	public Long getId() {
		return id;
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
