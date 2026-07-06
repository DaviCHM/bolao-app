package com.davi.poolbet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Side;

/**
 * Nucleo matematico do settlement parimutuel. Funcao <b>pura</b>: recebe as apostas
 * e o lado vencedor, e escreve o {@code payout} de cada aposta. Nao toca em banco,
 * transacao ou saldo — por isso e trivialmente testavel sem Spring nem mocks.
 *
 * <h2>Regra parimutuel</h2>
 * Para um mercado binario com {@code pool = total_A + total_B}, se o lado vencedor e W:
 * <pre>payout_i = valor_i / total_W * pool</pre>
 * A "odd" efetiva de cada lado ({@code pool / total_lado}) e <i>derivada</i> disso —
 * nunca armazenada. Apostar no lado com menos dinheiro paga um multiplicador maior;
 * isso e intrinseco a formula, nao ha campo de odd nem multiplicador extra.
 *
 * <h2>Casos de borda</h2>
 * <ul>
 *   <li><b>Sem apostas:</b> no-op.</li>
 *   <li><b>Lado vencedor vazio</b> ({@code total_W == 0}): divisao por zero — regra e
 *       reembolso total, cada apostador recebe {@code payout = valor} de volta.</li>
 *   <li><b>Lado perdedor vazio:</b> {@code total_W == pool}, multiplicador 1.0 — a formula
 *       ja devolve exatamente o valor apostado. Nenhum tratamento especial.</li>
 * </ul>
 *
 * <h2>Residuo de centavos (invariante critica)</h2>
 * A divisao gera dizimas. Arredondar cada payout isoladamente faria a soma nao bater
 * com o pool (centavos criados/destruidos). Usamos o <b>metodo do maior resto</b>
 * (largest remainder): trunca cada payout para baixo, e distribui os centavos que
 * sobraram, um a um, para os apostadores com maior parte fracionaria descartada.
 * Garante deterministicamente {@code soma(payouts) == pool} ao centavo.
 */
public final class SettlementCalculator {

	/** Escala monetaria: centavos. */
	private static final int MONEY_SCALE = 2;

	/** Escala interna para a divisao antes do truncamento (precisao de ranking do resto). */
	private static final int CALC_SCALE = 12;

	/** Um centavo. */
	private static final BigDecimal CENT = new BigDecimal("0.01");

	private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE);

	private SettlementCalculator() {
		// utilitario estatico
	}

	/**
	 * Calcula e grava o {@code payout} de cada aposta para o lado vencedor dado.
	 * Perdedores recebem {@code payout = 0}; vencedores recebem seu rateio do pool.
	 *
	 * @param bets        todas as apostas do mercado (mutadas in-place: seta {@code payout})
	 * @param winningSide lado vencedor (A ou B), obrigatorio
	 */
	public static void settle(List<Bet> bets, Side winningSide) {
		Objects.requireNonNull(winningSide, "winningSide");
		Objects.requireNonNull(bets, "bets");

		if (bets.isEmpty()) {
			// Mercado sem apostas: nada a fazer.
			return;
		}

		BigDecimal pool = ZERO_MONEY;
		BigDecimal totalWinner = ZERO_MONEY;
		for (Bet bet : bets) {
			pool = pool.add(bet.getValor());
			if (bet.getOpcao() == winningSide) {
				totalWinner = totalWinner.add(bet.getValor());
			}
		}

		// Caso de borda: ninguem apostou no lado vencedor -> reembolso total.
		// Cada apostador recebe exatamente o que apostou; nenhum saldo criado ou destruido.
		if (totalWinner.signum() == 0) {
			for (Bet bet : bets) {
				bet.setPayout(bet.getValor().setScale(MONEY_SCALE, RoundingMode.UNNECESSARY));
			}
			return;
		}

		// Caso normal. Perdedores: payout 0. Vencedores: rateio com largest remainder.
		List<Allotment> winners = new ArrayList<>();
		BigDecimal sumFloor = ZERO_MONEY;
		for (Bet bet : bets) {
			if (bet.getOpcao() != winningSide) {
				bet.setPayout(ZERO_MONEY);
				continue;
			}
			// payout exato = valor * pool / total_vencedor, com alta precisao.
			BigDecimal exact = bet.getValor()
					.multiply(pool)
					.divide(totalWinner, CALC_SCALE, RoundingMode.HALF_UP);
			// Trunca para baixo ao centavo; o que sobra vira "resto" a redistribuir.
			BigDecimal floor = exact.setScale(MONEY_SCALE, RoundingMode.DOWN);
			BigDecimal remainder = exact.subtract(floor);
			bet.setPayout(floor);
			sumFloor = sumFloor.add(floor);
			winners.add(new Allotment(bet, remainder));
		}

		// Centavos que sobraram apos o truncamento (sempre 0 <= residuo < numVencedores).
		BigDecimal residual = pool.subtract(sumFloor);
		int extraCents = residual.movePointRight(MONEY_SCALE).intValueExact();

		// Distribui um centavo por vez aos maiores restos (desempate: maior aposta, depois id).
		winners.sort(REMAINDER_DESC);
		for (int i = 0; i < extraCents && i < winners.size(); i++) {
			Bet bet = winners.get(i).bet;
			bet.setPayout(bet.getPayout().add(CENT));
		}
	}

	/** Par (aposta vencedora, parte fracionaria descartada no truncamento). */
	private static final class Allotment {
		final Bet bet;
		final BigDecimal remainder;

		Allotment(Bet bet, BigDecimal remainder) {
			this.bet = bet;
			this.remainder = remainder;
		}
	}

	/**
	 * Ordena vencedores para receber o centavo residual: maior resto primeiro; empate
	 * decidido pela maior aposta e, por fim, pelo menor id (nulls por ultimo) — deixando
	 * a distribuicao deterministica.
	 */
	private static final Comparator<Allotment> REMAINDER_DESC =
			Comparator.comparing((Allotment a) -> a.remainder).reversed()
					.thenComparing(Comparator.comparing((Allotment a) -> a.bet.getValor()).reversed())
					.thenComparing(a -> a.bet.getId(), Comparator.nullsLast(Comparator.naturalOrder()));
}
