package com.davi.poolbet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.davi.poolbet.exception.InsufficientBalanceException;
import com.davi.poolbet.exception.InvalidBetException;
import com.davi.poolbet.exception.MarketNotOpenException;
import com.davi.poolbet.exception.ResourceNotFoundException;
import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Market;
import com.davi.poolbet.model.MarketStatus;
import com.davi.poolbet.model.Side;
import com.davi.poolbet.model.User;
import com.davi.poolbet.repository.BetRepository;
import com.davi.poolbet.repository.MarketRepository;
import com.davi.poolbet.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fluxo de aposta. Contabilidade (modelo A): o valor sai do saldo <b>no momento da aposta</b>,
 * dentro de uma unica transacao — validar saldo, debitar e persistir a aposta sao atomicos.
 *
 * <p>A aposta adquire o mesmo lock pessimista do mercado usado na resolucao
 * ({@link MarketRepository#findByIdForUpdate}). Assim, uma aposta e um fechamento do mesmo
 * mercado nunca correm em paralelo: a aposta acontece inteiramente antes do fechamento
 * (e entra no rateio) ou depois (e rejeitada porque o mercado ja nao esta ABERTO).</p>
 */
@Service
public class BetService {

	private static final int MONEY_SCALE = 2;

	private final MarketRepository marketRepository;
	private final UserRepository userRepository;
	private final BetRepository betRepository;

	public BetService(MarketRepository marketRepository, UserRepository userRepository,
			BetRepository betRepository) {
		this.marketRepository = marketRepository;
		this.userRepository = userRepository;
		this.betRepository = betRepository;
	}

	/**
	 * Registra uma aposta de {@code userId} no lado {@code opcao} do mercado {@code marketId}.
	 * Mercado e usuario devem pertencer ao grupo autenticado (fora dele: 404).
	 *
	 * @throws InvalidBetException          valor nulo, <= 0 ou com mais de 2 casas decimais
	 * @throws ResourceNotFoundException    mercado ou usuario inexistente (ou de outro grupo)
	 * @throws MarketNotOpenException       mercado nao esta ABERTO
	 * @throws InsufficientBalanceException saldo insuficiente
	 */
	@Transactional
	public Bet placeBet(Long grupoId, Long marketId, Long userId, Side opcao, BigDecimal valor) {
		BigDecimal amount = normalizeAmount(valor);
		if (opcao == null) {
			throw new InvalidBetException("opcao (lado) e obrigatoria");
		}

		// Lock pessimista no mercado — serializa com a resolucao.
		Market market = marketRepository.findByIdForUpdate(marketId)
				.filter(m -> m.getGrupo().getId().equals(grupoId))
				.orElseThrow(() -> new ResourceNotFoundException("Mercado " + marketId + " nao encontrado"));
		if (market.getStatus() != MarketStatus.ABERTO) {
			throw new MarketNotOpenException(
					"Mercado " + marketId + " nao esta ABERTO (status atual: " + market.getStatus() + ")");
		}

		User user = userRepository.findById(userId)
				.filter(u -> u.getGrupo().getId().equals(grupoId))
				.orElseThrow(() -> new ResourceNotFoundException("Usuario " + userId + " nao encontrado"));

		if (user.getSaldo().compareTo(amount) < 0) {
			throw new InsufficientBalanceException(
					"Saldo insuficiente: saldo " + user.getSaldo() + ", aposta " + amount);
		}

		// Debita e persiste atomicamente.
		user.setSaldo(user.getSaldo().subtract(amount));
		return betRepository.save(new Bet(user, market, opcao, amount));
	}

	/** Valida o valor da aposta e o normaliza para 2 casas decimais. */
	private BigDecimal normalizeAmount(BigDecimal valor) {
		if (valor == null || valor.signum() <= 0) {
			throw new InvalidBetException("valor da aposta deve ser maior que zero");
		}
		try {
			return valor.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException e) {
			throw new InvalidBetException("valor da aposta deve ter no maximo 2 casas decimais");
		}
	}
}
