package com.davi.poolbet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.davi.poolbet.exception.InsufficientBalanceException;
import com.davi.poolbet.exception.InvalidBetException;
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
 * Testes do fluxo de aposta: debito atomico e validacoes (saldo, mercado aberto, valor).
 */
@ExtendWith(MockitoExtension.class)
class BetServiceTest {

	private static final long MARKET_ID = 1L;
	private static final long USER_ID = 42L;
	private static final long GRUPO_ID = 7L;

	@Mock
	private MarketRepository marketRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private BetRepository betRepository;

	@InjectMocks
	private BetService betService;

	private User user;
	private Market market;

	@BeforeEach
	void setUp() {
		Grupo grupo = new Grupo("amigos", "hash");
		ReflectionTestUtils.setField(grupo, "id", GRUPO_ID);
		user = new User(grupo, "alice", new BigDecimal("100.00"));
		market = new Market("Vai chover?", "Sim", "Nao",
				new User(grupo, "criador", new BigDecimal("1000.00")));
	}

	@Test
	@DisplayName("aposta valida debita o saldo e persiste")
	void apostaValida() {
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(betRepository.save(any(Bet.class))).thenAnswer(inv -> inv.getArgument(0));

		Bet bet = betService.placeBet(GRUPO_ID, MARKET_ID, USER_ID, Side.A, new BigDecimal("30"));

		assertThat(user.getSaldo()).isEqualByComparingTo("70.00"); // 100 - 30
		assertThat(bet.getUser()).isSameAs(user);
		assertThat(bet.getMarket()).isSameAs(market);
		assertThat(bet.getOpcao()).isEqualTo(Side.A);
		assertThat(bet.getValor()).isEqualByComparingTo("30.00");
		assertThat(bet.getPayout()).isNull(); // so definido na resolucao
	}

	@Test
	@DisplayName("saldo insuficiente: rejeita, saldo intacto, nada persistido")
	void saldoInsuficiente() {
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> betService.placeBet(GRUPO_ID, MARKET_ID, USER_ID, Side.A, new BigDecimal("150")))
				.isInstanceOf(InsufficientBalanceException.class);

		assertThat(user.getSaldo()).isEqualByComparingTo("100.00"); // intacto
		verify(betRepository, never()).save(any());
	}

	@Test
	@DisplayName("mercado fechado: rejeita a aposta")
	void mercadoFechado() {
		market.setStatus(MarketStatus.FECHADO);
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));

		assertThatThrownBy(() -> betService.placeBet(GRUPO_ID, MARKET_ID, USER_ID, Side.A, new BigDecimal("10")))
				.isInstanceOf(MarketNotOpenException.class);

		assertThat(user.getSaldo()).isEqualByComparingTo("100.00");
		verify(betRepository, never()).save(any());
	}

	@Test
	@DisplayName("mercado inexistente: ResourceNotFoundException")
	void mercadoInexistente() {
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> betService.placeBet(GRUPO_ID, MARKET_ID, USER_ID, Side.A, new BigDecimal("10")))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("usuario inexistente: ResourceNotFoundException")
	void usuarioInexistente() {
		when(marketRepository.findByIdForUpdate(MARKET_ID)).thenReturn(Optional.of(market));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> betService.placeBet(GRUPO_ID, MARKET_ID, USER_ID, Side.A, new BigDecimal("10")))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("valor <= 0: InvalidBetException, sem nem tocar no mercado")
	void valorNaoPositivo() {
		assertThatThrownBy(() -> betService.placeBet(GRUPO_ID, MARKET_ID, USER_ID, Side.A, new BigDecimal("0")))
				.isInstanceOf(InvalidBetException.class);
		assertThatThrownBy(() -> betService.placeBet(GRUPO_ID, MARKET_ID, USER_ID, Side.A, new BigDecimal("-5")))
				.isInstanceOf(InvalidBetException.class);

		verify(marketRepository, never()).findByIdForUpdate(any());
	}

	@Test
	@DisplayName("valor com mais de 2 casas decimais: InvalidBetException")
	void valorComPrecisaoInvalida() {
		assertThatThrownBy(() -> betService.placeBet(GRUPO_ID, MARKET_ID, USER_ID, Side.A, new BigDecimal("10.005")))
				.isInstanceOf(InvalidBetException.class);

		verify(marketRepository, never()).findByIdForUpdate(any());
	}
}
