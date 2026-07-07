package com.davi.poolbet.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do nucleo matematico do settlement com casamento proporcional (all-in).
 * A invariante central e: <b>soma(payouts) == pool</b> ao centavo, em todo cenario
 * com vencedor; e ninguem perde mais do que o lado adversario arriscou.
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
	@DisplayName("Caso base 100/900: vencedores ganham 2x (casado = 100), perdedor recebe o excedente")
	void casoBase() {
		// total_A = 100 (10 + 90), total_B = 900, pool = 1000. A vence. Casado M = 100.
		Bet menor = bet(Side.A, "10");
		Bet maior = bet(Side.A, "90");
		Bet perdedorB = bet(Side.B, "900");
		List<Bet> bets = List.of(menor, maior, perdedorB);

		SettlementCalculator.settle(bets, Side.A);

		assertThat(menor.getPayout()).isEqualByComparingTo("20.00");     // 10 + 10*100/100 (2x)
		assertThat(maior.getPayout()).isEqualByComparingTo("180.00");    // 90 + 90*100/100 (2x)
		assertThat(perdedorB.getPayout()).isEqualByComparingTo("800.00"); // 900 - casado 100
		assertThat(somaPayouts(bets)).isEqualByComparingTo("1000.00");
		assertThat(somaPayouts(bets)).isEqualByComparingTo(pool(bets)); // invariante
	}

	@Test
	@DisplayName("Cap estilo poker: quem aposta mais que o outro lado so arrisca o casado")
	void capDoCasamento() {
		// X aposta 800 em A; Y (200) e Z (100) em B. Casado M = 300.
		Bet x = bet(Side.A, "800");
		Bet y = bet(Side.B, "200");
		Bet z = bet(Side.B, "100");

		// B vence: X perde apenas o casado (300), nao os 800.
		List<Bet> bets = List.of(x, y, z);
		SettlementCalculator.settle(bets, Side.B);
		assertThat(x.getPayout()).isEqualByComparingTo("500.00"); // 800 - 800*300/800
		assertThat(y.getPayout()).isEqualByComparingTo("400.00"); // 200 + 200*300/300
		assertThat(z.getPayout()).isEqualByComparingTo("200.00"); // 100 + 100*300/300
		assertThat(somaPayouts(bets)).isEqualByComparingTo(pool(bets));

		// A vence: X lucra exatamente o que o outro lado arriscou (300).
		SettlementCalculator.settle(bets, Side.A);
		assertThat(x.getPayout()).isEqualByComparingTo("1100.00"); // 800 + 300
		assertThat(y.getPayout()).isEqualByComparingTo("0.00");
		assertThat(z.getPayout()).isEqualByComparingTo("0.00");
		assertThat(somaPayouts(bets)).isEqualByComparingTo(pool(bets));
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
		// Todos apostaram em A; A vence. Casado M = 0.
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
		// Vencedores 10/20/30 em A (total 60), perdedor 5 em B. Casado M = 5.
		//   10 + 10*5/60 = 10.8333.. -> floor 10.83 (resto .3333)
		//   20 + 20*5/60 = 21.6667.. -> floor 21.66 (resto .6667)  <- maior resto
		//   30 + 30*5/60 = 32.5000   -> floor 32.50 (resto .5000)
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
