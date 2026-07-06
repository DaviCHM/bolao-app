package com.davi.poolbet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes da orquestracao transacional: guarda de idempotencia (nao resolver duas vezes),
 * credito de saldo aos vencedores e cancelamento com reembolso.
 */
@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

	private static final long MARKET_ID = 1L;

	@Mock
	private MarketRepository marketRepository;

	@Mock
	private BetRepository betRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private MarketService marketService;

	private User criador;

	@BeforeEach
	void setUp() {
		criador = new User("criador", new BigDecimal("1000.00"));
	}

	private Market abertoMarket() {
		return new Market("Vai chover amanha?", "Sim", "Nao", criador);
	}

	private Bet bet(User user, Side side, String valor) {
		return new Bet(user, abertoMarket(), side, new BigDecimal(valor));
	}

	@Test
	@DisplayName("resolve credita payout aos vencedores e fecha o mercado")
	void resolveCreditaVencedores() {
		Market market = abertoMarket();
		User alice = new User("alice", new BigDecimal("500.00"));
		User bob = new User("bob", new BigDecimal("500.00"));
		User carol = new User("carol", new BigDecimal("500.00"));
		// total_A = 100, total_B = 900, pool = 1000. A vence.
		List<Bet> bets = List.of(
				bet(alice, Side.A, "10"),
				bet(bob, Side.A, "90"),
				bet(carol, Side.B, "900"));
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(betRepository.findByMarketId(MARKET_ID)).thenReturn(bets);

		marketService.resolveMarket(MARKET_ID, Side.A);

		assertThat(market.getStatus()).isEqualTo(MarketStatus.FECHADO);
		assertThat(market.getResultado()).isEqualTo(Side.A);
		assertThat(alice.getSaldo()).isEqualByComparingTo("600.00"); // 500 + 100
		assertThat(bob.getSaldo()).isEqualByComparingTo("1400.00");  // 500 + 900
		assertThat(carol.getSaldo()).isEqualByComparingTo("500.00"); // perdeu, sem credito
	}

	@Test
	@DisplayName("resolver mercado ja FECHADO lanca e nao toca em saldo nem carrega apostas")
	void resolverMercadoJaFechado() {
		Market market = abertoMarket();
		market.setStatus(MarketStatus.FECHADO);
		market.setResultado(Side.A);
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));

		assertThatThrownBy(() -> marketService.resolveMarket(MARKET_ID, Side.B))
				.isInstanceOf(MarketNotOpenException.class);

		// Guarda de idempotencia: nem chegou a carregar apostas (nenhum credito possivel).
		verify(betRepository, never()).findByMarketId(any());
		assertThat(criador.getSaldo()).isEqualByComparingTo("1000.00");
	}

	@Test
	@DisplayName("resolver mercado inexistente lanca ResourceNotFoundException")
	void resolverMercadoInexistente() {
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> marketService.resolveMarket(MARKET_ID, Side.A))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("resolver mercado sem apostas e no-op: fecha sem mexer em saldo")
	void resolverSemApostas() {
		Market market = abertoMarket();
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(betRepository.findByMarketId(MARKET_ID)).thenReturn(List.of());

		marketService.resolveMarket(MARKET_ID, Side.A);

		assertThat(market.getStatus()).isEqualTo(MarketStatus.FECHADO);
		assertThat(market.getResultado()).isEqualTo(Side.A);
	}

	@Test
	@DisplayName("cancelar reembolsa todos e marca CANCELADO sem vencedor")
	void cancelarReembolsaTodos() {
		Market market = abertoMarket();
		User alice = new User("alice", new BigDecimal("500.00"));
		User bob = new User("bob", new BigDecimal("500.00"));
		List<Bet> bets = List.of(
				bet(alice, Side.A, "120"),
				bet(bob, Side.B, "80"));
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(betRepository.findByMarketId(MARKET_ID)).thenReturn(bets);

		marketService.cancelMarket(MARKET_ID);

		assertThat(market.getStatus()).isEqualTo(MarketStatus.CANCELADO);
		assertThat(market.getResultado()).isNull();
		assertThat(alice.getSaldo()).isEqualByComparingTo("620.00"); // 500 + 120 devolvido
		assertThat(bob.getSaldo()).isEqualByComparingTo("580.00");   // 500 + 80 devolvido
	}
}
