package com.davi.poolbet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Mercado binario: uma pergunta com dois lados ({@code opcaoA} / {@code opcaoB}).
 * E o aggregate root do settlement — o lock pessimista adquirido na resolucao/aposta
 * incide nesta linha, serializando toda mutacao do mercado num unico ponto.
 */
@Entity
@Table(name = "markets")
public class Market {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String pergunta;

	@Column(name = "opcao_a", nullable = false)
	private String opcaoA;

	@Column(name = "opcao_b", nullable = false)
	private String opcaoB;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MarketStatus status = MarketStatus.ABERTO;

	/** Lado vencedor; nulo enquanto ABERTO ou se CANCELADO. */
	@Enumerated(EnumType.STRING)
	@Column(length = 1)
	private Side resultado;

	@ManyToOne(optional = false)
	@JoinColumn(name = "criador_id", nullable = false)
	private User criador;

	/** Grupo dono do mercado (sempre o grupo do criador); base do isolamento entre grupos. */
	@ManyToOne(optional = false)
	@JoinColumn(name = "grupo_id", nullable = false)
	private Grupo grupo;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Market() {
		// JPA
	}

	public Market(String pergunta, String opcaoA, String opcaoB, User criador) {
		this.pergunta = pergunta;
		this.opcaoA = opcaoA;
		this.opcaoB = opcaoB;
		this.criador = criador;
		this.grupo = criador.getGrupo();
		this.status = MarketStatus.ABERTO;
	}

	public Long getId() {
		return id;
	}

	public String getPergunta() {
		return pergunta;
	}

	public String getOpcaoA() {
		return opcaoA;
	}

	public String getOpcaoB() {
		return opcaoB;
	}

	public MarketStatus getStatus() {
		return status;
	}

	public void setStatus(MarketStatus status) {
		this.status = status;
	}

	public Side getResultado() {
		return resultado;
	}

	public void setResultado(Side resultado) {
		this.resultado = resultado;
	}

	public User getCriador() {
		return criador;
	}

	public Grupo getGrupo() {
		return grupo;
	}

	public Long getVersion() {
		return version;
	}
}
