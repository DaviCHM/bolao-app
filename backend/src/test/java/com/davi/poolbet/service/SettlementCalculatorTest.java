package com.davi.poolbet.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do nucleo matematico do settlement parimutuel. A invariante central e:
 * <b>soma(payouts) == pool</b> ao centavo, em todo cenario com vencedor.
 */
class SettlementCalculatorTest {

	/** Cria uma aposta desacoplada de User/Market (o calculator so olha valor/opcao). */
	private static Bet bet(Side side, String valor) {
		return new Bet(null, null, side, new BigDecimal(valor));
	}

	private static BigDecimal somaPayouts(List<Bet> bets) {
		return bets.stream()
				.map(Bet::getPayout)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal pool(List<Bet> bets) {
		return bets.stream()
				.map(Bet::getValor)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Test
	@DisplayName("Caso base 100/900: azarao recebe 10x, favorito 9x, soma == pool")
	void casoBase() {
		// total_A = 100 (10 + 90), total_B = 900, pool = 1000. A vence.
		Bet azarao = bet(Side.A, "10");
		Bet favorito = bet(Side.A, "90");
		Bet perdedorB = bet(Side.B, "900");
		List<Bet> bets = List.of(azarao, favorito, perdedorB);

		SettlementCalculator.settle(bets, Side.A);

		assertThat(azarao.getPayout()).isEqualByComparingTo("100.00");   // (10/100)*1000 = 10x
		assertThat(favorito.getPayout()).isEqualByComparingTo("900.00"); // (90/100)*1000 = 9x
		assertThat(perdedorB.getPayout()).isEqualByComparingTo("0.00");
		assertThat(somaPayouts(bets)).isEqualByComparingTo("1000.00");
		assertThat(somaPayouts(bets)).isEqualByComparingTo(pool(bets)); // invariante
	}

	@Test
	@DisplayName("Lado vencedor vazio: reembolso total, cada um recebe o que apostou")
	void ladoVencedorVazio() {
		// Ninguem apostou em A; A vence -> divisao por zero -> refund.
		Bet b1 = bet(Side.B, "300");
		Bet b2 = bet(Side.B, "700");
		List<Bet> bets = List.of(b1, b2);

		SettlementCalculator.settle(bets, Side.A);

		assertThat(b1.getPayout()).isEqualByComparingTo("300.00");
		assertThat(b2.getPayout()).isEqualByComparingTo("700.00");
		assertThat(somaPayouts(bets)).isEqualByComparingTo(pool(bets)); // nada criado nem destruido
	}

	@Test
	@DisplayName("Lado perdedor vazio: multiplicador 1.0, cada um recebe seu valor de volta")
	void ladoPerdedorVazio() {
		// Todos apostaram em A; A vence. total_vencedor == pool.
		Bet a1 = bet(Side.A, "150");
		Bet a2 = bet(Side.A, "250");
		List<Bet> bets = List.of(a1, a2);

		SettlementCalculator.settle(bets, Side.A);

		assertThat(a1.getPayout()).isEqualByComparingTo("150.00");
		assertThat(a2.getPayout()).isEqualByComparingTo("250.00");
		assertThat(somaPayouts(bets)).isEqualByComparingTo(pool(bets));
	}

	@Test
	@DisplayName("Mercado sem apostas: no-op, nao lanca")
	void semApostas() {
		SettlementCalculator.settle(List.of(), Side.A);
		// sem excecao; nada a assertar
	}

	@Test
	@DisplayName("Residuo de centavos: dizima nao vaza um centavo; sobra vai ao maior resto")
	void residuoDeCentavos() {
		// Vencedores 10/20/30 em A (total 60), perdedor 5 em B, pool = 65.
		//   10/60*65 = 10.8333.. -> floor 10.83 (resto .3333)
		//   20/60*65 = 21.6667.. -> floor 21.66 (resto .6667)  <- maior resto
		//   30/60*65 = 32.5000   -> floor 32.50 (resto .5000)
		// soma floors = 64.99; residuo 0.01 -> vai para o de maior resto (o de 20).
		Bet w10 = bet(Side.A, "10");
		Bet w20 = bet(Side.A, "20");
		Bet w30 = bet(Side.A, "30");
		Bet loser = bet(Side.B, "5");
		List<Bet> bets = List.of(w10, w20, w30, loser);

		SettlementCalculator.settle(bets, Side.A);

		assertThat(w10.getPayout()).isEqualByComparingTo("10.83");
		assertThat(w20.getPayout()).isEqualByComparingTo("21.67"); // recebeu o centavo residual
		assertThat(w30.getPayout()).isEqualByComparingTo("32.50");
		assertThat(loser.getPayout()).isEqualByComparingTo("0.00");
		// Invariante: nenhum centavo criado ou destruido.
		assertThat(somaPayouts(bets)).isEqualByComparingTo("65.00");
		assertThat(somaPayouts(bets)).isEqualByComparingTo(pool(bets));
	}

	@Test
	@DisplayName("Invariante soma == pool vale para varias distribuicoes com dizima")
	void invarianteVariasDistribuicoes() {
		String[][] cenarios = {
				{"1", "1", "1"},      // 3 vencedores iguais, dizima classica 1/3
				{"7", "11", "13"},    // primos
				{"0.01", "0.02", "100"},
				{"33.33", "66.67", "0.01"},
		};
		for (String[] valores : cenarios) {
			Bet w1 = bet(Side.A, valores[0]);
			Bet w2 = bet(Side.A, valores[1]);
			Bet loserB = bet(Side.B, valores[2]);
			List<Bet> bets = List.of(w1, w2, loserB);

			SettlementCalculator.settle(bets, Side.A);

			assertThat(somaPayouts(bets))
					.as("soma dos payouts deve bater com o pool para %s", (Object) valores)
					.isEqualByComparingTo(pool(bets));
		}
	}
}
