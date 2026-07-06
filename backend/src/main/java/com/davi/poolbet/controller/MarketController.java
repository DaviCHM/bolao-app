package com.davi.poolbet.controller;

import java.util.List;

import com.davi.poolbet.dto.BetResponse;
import com.davi.poolbet.dto.CancelMarketRequest;
import com.davi.poolbet.dto.CreateMarketRequest;
import com.davi.poolbet.dto.MarketDetailResponse;
import com.davi.poolbet.dto.MarketSummaryResponse;
import com.davi.poolbet.dto.PlaceBetRequest;
import com.davi.poolbet.dto.ResolveMarketRequest;
import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Market;
import com.davi.poolbet.model.MarketStatus;
import com.davi.poolbet.service.BetService;
import com.davi.poolbet.service.MarketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/markets")
public class MarketController {

	private final MarketService marketService;
	private final BetService betService;

	public MarketController(MarketService marketService, BetService betService) {
		this.marketService = marketService;
		this.betService = betService;
	}

	@PostMapping
	public ResponseEntity<MarketSummaryResponse> create(@Valid @RequestBody CreateMarketRequest request) {
		Market market = marketService.createMarket(
				request.pergunta(), request.opcaoA(), request.opcaoB(), request.criadorId());
		MarketSummaryResponse body = MarketSummaryResponse.of(market, List.of());
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	/** Lista mercados com metricas derivadas. Filtro opcional por status (?status=ABERTO). */
	@GetMapping
	public List<MarketSummaryResponse> list(@RequestParam(required = false) MarketStatus status) {
		// N+1 assumido (uma consulta de apostas por mercado): simples e legivel para a escala do projeto.
		return marketService.listMarkets(status).stream()
				.map(market -> MarketSummaryResponse.of(market, marketService.betsOf(market.getId())))
				.toList();
	}

	@GetMapping("/{id}")
	public MarketDetailResponse get(@PathVariable Long id) {
		return detail(id);
	}

	@PostMapping("/{id}/bets")
	public ResponseEntity<BetResponse> placeBet(
			@PathVariable Long id, @Valid @RequestBody PlaceBetRequest request) {
		Bet bet = betService.placeBet(id, request.usuarioId(), request.opcao(), request.valor());
		return ResponseEntity.status(HttpStatus.CREATED).body(BetResponse.from(bet));
	}

	@PostMapping("/{id}/resolve")
	public MarketDetailResponse resolve(
			@PathVariable Long id, @Valid @RequestBody ResolveMarketRequest request) {
		marketService.assertIsCreator(id, request.solicitanteId());
		marketService.resolveMarket(id, request.resultado());
		return detail(id);
	}

	@PostMapping("/{id}/cancel")
	public MarketDetailResponse cancel(
			@PathVariable Long id, @Valid @RequestBody CancelMarketRequest request) {
		marketService.assertIsCreator(id, request.solicitanteId());
		marketService.cancelMarket(id);
		return detail(id);
	}

	/** Monta o detalhe do mercado (resumo + apostas) numa leitura fresca do estado. */
	private MarketDetailResponse detail(Long id) {
		Market market = marketService.getMarket(id);
		return MarketDetailResponse.of(market, marketService.betsOf(id));
	}
}
