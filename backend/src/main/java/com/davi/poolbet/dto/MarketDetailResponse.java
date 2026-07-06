package com.davi.poolbet.dto;

import java.util.List;

import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Market;

/**
 * Detalhe do mercado: o resumo com metricas derivadas mais o breakdown das apostas.
 */
public record MarketDetailResponse(
		MarketSummaryResponse mercado,
		List<BetResponse> apostas) {

	public static MarketDetailResponse of(Market market, List<Bet> bets) {
		List<BetResponse> apostas = bets.stream().map(BetResponse::from).toList();
		return new MarketDetailResponse(MarketSummaryResponse.of(market, bets), apostas);
	}
}
