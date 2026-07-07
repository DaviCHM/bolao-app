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
import com.davi.poolbet.model.Grupo;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Testes da orquestracao transacional: guarda de idempotencia (nao resolver duas vezes),
 * credito de saldo aos vencedores e cancelamento com reembolso.
 */
@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

	private static final long MARKET_ID = 1L;
	private static final long GRUPO_ID = 7L;

	@Mock
	private MarketRepository marketRepository;

	@Mock
	private BetRepository betRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private MarketService marketService;

	private Grupo grupo;
	private User criador;

	@BeforeEach
	void setUp() {
		grupo = new Grupo("amigos", "hash");
		ReflectionTestUtils.setField(grupo, "id", GRUPO_ID);
		criador = new User(grupo, "criador", new BigDecimal("1000.00"));
	}

	private User user(String nome, String saldo) {
		return new User(grupo, nome, new BigDecimal(saldo));
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
		User alice = user("alice", "500.00");
		User bob = user("bob", "500.00");
		User carol = user("carol", "500.00");
		// total_A = 100, total_B = 900, casado M = 100. A vence.
		List<Bet> bets = List.of(
				bet(alice, Side.A, "10"),
				bet(bob, Side.A, "90"),
				bet(carol, Side.B, "900"));
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(betRepository.findByMarketId(MARKET_ID)).thenReturn(bets);

		marketService.resolveMarket(GRUPO_ID, MARKET_ID, Side.A);

		assertThat(market.getStatus()).isEqualTo(MarketStatus.FECHADO);
		assertThat(market.getResultado()).isEqualTo(Side.A);
		assertThat(alice.getSaldo()).isEqualByComparingTo("520.00");  // 500 + 20 (2x de 10)
		assertThat(bob.getSaldo()).isEqualByComparingTo("680.00");    // 500 + 180 (2x de 90)
		assertThat(carol.getSaldo()).isEqualByComparingTo("1300.00"); // 500 + 800 (excedente devolvido)
	}

	@Test
	@DisplayName("resolver mercado ja FECHADO lanca e nao toca em saldo nem carrega apostas")
	void resolverMercadoJaFechado() {
		Market market = abertoMarket();
		market.setStatus(MarketStatus.FECHADO);
		market.setResultado(Side.A);
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));

		assertThatThrownBy(() -> marketService.resolveMarket(GRUPO_ID, MARKET_ID, Side.B))
				.isInstanceOf(MarketNotOpenException.class);

		// Guarda de idempotencia: nem chegou a carregar apostas (nenhum credito possivel).
		verify(betRepository, never()).findByMarketId(any());
		assertThat(criador.getSaldo()).isEqualByComparingTo("1000.00");
	}

	@Test
	@DisplayName("resolver mercado inexistente lanca ResourceNotFoundException")
	void resolverMercadoInexistente() {
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> marketService.resolveMarket(GRUPO_ID, MARKET_ID, Side.A))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("resolver mercado sem apostas e no-op: fecha sem mexer em saldo")
	void resolverSemApostas() {
		Market market = abertoMarket();
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(betRepository.findByMarketId(MARKET_ID)).thenReturn(List.of());

		marketService.resolveMarket(GRUPO_ID, MARKET_ID, Side.A);

		assertThat(market.getStatus()).isEqualTo(MarketStatus.FECHADO);
		assertThat(market.getResultado()).isEqualTo(Side.A);
	}

	@Test
	@DisplayName("cancelar reembolsa todos e marca CANCELADO sem vencedor")
	void cancelarReembolsaTodos() {
		Market market = abertoMarket();
		User alice = user("alice", "500.00");
		User bob = user("bob", "500.00");
		List<Bet> bets = List.of(
				bet(alice, Side.A, "120"),
				bet(bob, Side.B, "80"));
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(betRepository.findByMarketId(MARKET_ID)).thenReturn(bets);

		marketService.cancelMarket(GRUPO_ID, MARKET_ID);

		assertThat(market.getStatus()).isEqualTo(MarketStatus.CANCELADO);
		assertThat(market.getResultado()).isNull();
		assertThat(alice.getSaldo()).isEqualByComparingTo("620.00"); // 500 + 120 devolvido
		assertThat(bob.getSaldo()).isEqualByComparingTo("580.00");   // 500 + 80 devolvido
	}
}
