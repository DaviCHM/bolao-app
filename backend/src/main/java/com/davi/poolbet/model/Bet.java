package com.davi.poolbet.model;

import java.math.BigDecimal;

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

/**
 * Aposta de um usuario num lado de um mercado.
 *
 * <p>Contabilidade (modelo A): o {@code valor} sai do saldo no momento da aposta.
 * O {@code payout} e nulo ate a resolucao e, no fechamento, e gravado como retrato
 * imutavel do que foi creditado (auditoria da invariante soma(payouts) == pool).</p>
 */
@Entity
@Table(name = "bets")
public class Bet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(optional = false)
	@JoinColumn(name = "market_id", nullable = false)
	private Market market;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 1)
	private Side opcao;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal valor;

	/** Valor creditado ao usuario na resolucao. Nulo enquanto o mercado nao foi resolvido. */
	@Column(precision = 19, scale = 2)
	private BigDecimal payout;

	protected Bet() {
		// JPA
	}

	public Bet(User user, Market market, Side opcao, BigDecimal valor) {
		this.user = user;
		this.market = market;
		this.opcao = opcao;
		this.valor = valor;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Market getMarket() {
		return market;
	}

	public Side getOpcao() {
		return opcao;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public BigDecimal getPayout() {
		return payout;
	}

	public void setPayout(BigDecimal payout) {
		this.payout = payout;
	}
}
