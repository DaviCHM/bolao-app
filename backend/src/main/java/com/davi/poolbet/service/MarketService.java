package com.davi.poolbet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.davi.poolbet.exception.MarketNotOpenException;
import com.davi.poolbet.exception.ResourceNotFoundException;
import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Market;
import com.davi.poolbet.model.MarketStatus;
import com.davi.poolbet.model.Side;
import com.davi.poolbet.repository.BetRepository;
import com.davi.poolbet.repository.MarketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestracao transacional do ciclo de vida do mercado. A matematica do rateio vive
 * no {@link SettlementCalculator} (funcao pura); aqui ficam a transacao, o lock e a
 * contabilidade de saldo.
 *
 * <h2>Contabilidade (modelo A)</h2>
 * O valor da aposta ja saiu do saldo no momento da aposta (ver camada de aposta). Na
 * resolucao, credita-se {@code payout} a cada usuario: vencedores recebem seu rateio,
 * perdedores recebem 0 (ja pagaram), e no reembolso todos recebem o valor de volta.
 *
 * <h2>Concorrencia</h2>
 * {@link MarketRepository#findByIdForUpdate} adquire lock pessimista na linha do mercado,
 * serializando aposta e resolucao do mesmo mercado. O saldo do usuario tem lock otimista
 * ({@code @Version}), cobrindo o caso de apostas concorrentes em mercados diferentes.
 */
@Service
public class MarketService {

	private static final int MONEY_SCALE = 2;

	private final MarketRepository marketRepository;
	private final BetRepository betRepository;

	public MarketService(MarketRepository marketRepository, BetRepository betRepository) {
		this.marketRepository = marketRepository;
		this.betRepository = betRepository;
	}

	/**
	 * Resolve o mercado com o lado vencedor e liquida os payouts numa unica transacao.
	 * Se qualquer passo falhar, rollback total — nenhum saldo fica inconsistente.
	 *
	 * @throws ResourceNotFoundException mercado inexistente
	 * @throws MarketNotOpenException    mercado nao esta ABERTO (guarda contra dupla liquidacao)
	 */
	@Transactional
	public Market resolveMarket(Long marketId, Side resultado) {
		if (resultado == null) {
			throw new IllegalArgumentException("resultado (lado vencedor) e obrigatorio");
		}
		Market market = lockOpenMarket(marketId);

		List<Bet> bets = betRepository.findByMarketId(marketId);
		SettlementCalculator.settle(bets, resultado);
		creditPayouts(bets);

		market.setResultado(resultado);
		market.setStatus(MarketStatus.FECHADO);
		return market;
	}

	/**
	 * Cancela (anula) o mercado: reembolsa a todos exatamente o que apostaram e nenhum
	 * saldo liquido muda. Util quando o evento nao ocorreu.
	 *
	 * @throws ResourceNotFoundException mercado inexistente
	 * @throws MarketNotOpenException    mercado nao esta ABERTO
	 */
	@Transactional
	public Market cancelMarket(Long marketId) {
		Market market = lockOpenMarket(marketId);

		List<Bet> bets = betRepository.findByMarketId(marketId);
		for (Bet bet : bets) {
			bet.setPayout(bet.getValor().setScale(MONEY_SCALE, RoundingMode.UNNECESSARY));
		}
		creditPayouts(bets);

		market.setResultado(null);
		market.setStatus(MarketStatus.CANCELADO);
		return market;
	}

	/** Carrega o mercado com lock pessimista e valida que esta ABERTO. */
	private Market lockOpenMarket(Long marketId) {
		Market market = marketRepository.findByIdForUpdate(marketId)
				.orElseThrow(() -> new ResourceNotFoundException("Mercado " + marketId + " nao encontrado"));
		if (market.getStatus() != MarketStatus.ABERTO) {
			throw new MarketNotOpenException(
					"Mercado " + marketId + " nao esta ABERTO (status atual: " + market.getStatus() + ")");
		}
		return market;
	}

	/** Credita o payout de cada aposta no saldo do respectivo usuario (dirty checking persiste). */
	private void creditPayouts(List<Bet> bets) {
		for (Bet bet : bets) {
			BigDecimal payout = bet.getPayout();
			if (payout != null && payout.signum() > 0) {
				var user = bet.getUser();
				user.setSaldo(user.getSaldo().add(payout));
			}
		}
	}
}
