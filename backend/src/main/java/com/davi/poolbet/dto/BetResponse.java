package com.davi.poolbet.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Side;

/**
 * Aposta exposta na API. O {@code multiplicador} efetivo (payout/valor) so existe apos a
 * resolucao; e nulo enquanto o mercado esta aberto.
 */
public record BetResponse(
		Long id,
		Long usuarioId,
		String usuarioNome,
		Side opcao,
		BigDecimal valor,
		BigDecimal payout,
		BigDecimal multiplicador) {

	public static BetResponse from(Bet bet) {
		BigDecimal multiplicador = null;
		if (bet.getPayout() != null && bet.getValor().signum() > 0) {
			multiplicador = bet.getPayout().divide(bet.getValor(), 2, RoundingMode.HALF_UP);
		}
		return new BetResponse(
				bet.getId(),
				bet.getUser().getId(),
				bet.getUser().getNome(),
				bet.getOpcao(),
				bet.getValor(),
				bet.getPayout(),
				multiplicador);
	}
}
