package com.davi.poolbet.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Market;
import com.davi.poolbet.model.Side;

/**
 * Mercado com suas metricas <b>derivadas</b> (nao persistidas), calculadas on-read a partir
 * das apostas:
 * <ul>
 *   <li>{@code totalA/totalB/pool} — dinheiro em cada lado e o total.</li>
 *   <li>{@code oddA/oddB} — multiplicador efetivo do lado com o casamento proporcional:
 *       {@code 1 + min(totalA, totalB) / total_lado} (maximo 2x); {@code null} se o lado
 *       esta vazio.</li>
 *   <li>{@code probA/probB} — fatia do pool em cada lado ({@code total_lado/pool}), no
 *       intervalo [0,1]; {@code null} se nao ha apostas.</li>
 * </ul>
 */
public record MarketSummaryResponse(
		Long id,
		String pergunta,
		String opcaoA,
		String opcaoB,
		String status,
		Side resultado,
		Long criadorId,
		BigDecimal totalA,
		BigDecimal totalB,
		BigDecimal pool,
		BigDecimal oddA,
		BigDecimal oddB,
		BigDecimal probA,
		BigDecimal probB) {

	private static final int ODD_SCALE = 2;
	private static final int PROB_SCALE = 4;

	public static MarketSummaryResponse of(Market market, List<Bet> bets) {
		BigDecimal totalA = BigDecimal.ZERO.setScale(2);
		BigDecimal totalB = BigDecimal.ZERO.setScale(2);
		for (Bet bet : bets) {
			if (bet.getOpcao() == Side.A) {
				totalA = totalA.add(bet.getValor());
			} else {
				totalB = totalB.add(bet.getValor());
			}
		}
		BigDecimal pool = totalA.add(totalB);
		BigDecimal matched = totalA.min(totalB);

		return new MarketSummaryResponse(
				market.getId(),
				market.getPergunta(),
				market.getOpcaoA(),
				market.getOpcaoB(),
				market.getStatus().name(),
				market.getResultado(),
				market.getCriador().getId(),
				totalA,
				totalB,
				pool,
				odd(matched, totalA),
				odd(matched, totalB),
				prob(totalA, pool),
				prob(totalB, pool));
	}

	/** Multiplicador efetivo do lado = 1 + casado/total_lado (max 2x); null se o lado esta vazio. */
	private static BigDecimal odd(BigDecimal matched, BigDecimal totalLado) {
		if (totalLado.signum() == 0) {
			return null;
		}
		return BigDecimal.ONE.add(matched.divide(totalLado, ODD_SCALE, RoundingMode.HALF_UP))
				.setScale(ODD_SCALE, RoundingMode.HALF_UP);
	}

	/** Fatia do pool no lado = total_lado / pool; null se nao ha apostas. */
	private static BigDecimal prob(BigDecimal totalLado, BigDecimal pool) {
		if (pool.signum() == 0) {
			return null;
		}
		return totalLado.divide(pool, PROB_SCALE, RoundingMode.HALF_UP);
	}
}
