package com.davi.poolbet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.davi.poolbet.model.Bet;
import com.davi.poolbet.model.Side;

/**
 * Nucleo matematico do settlement com <b>casamento proporcional</b> (estilo all-in do
 * poker). Funcao <b>pura</b>: recebe as apostas e o lado vencedor, e escreve o
 * {@code payout} de cada aposta. Nao toca em banco, transacao ou saldo — por isso e
 * trivialmente testavel sem Spring nem mocks.
 *
 * <h2>Regra do casamento</h2>
 * So entra em jogo o que o outro lado consegue cobrir. Com {@code M = min(total_A, total_B)}:
 * <pre>
 * vencedor: payout_i = valor_i + valor_i * M / total_vencedor   (multiplicador maximo 2x)
 * perdedor: payout_j = valor_j - valor_j * M / total_perdedor   (excedente nao casado volta)
 * </pre>
 * Ninguem pode perder mais do que o lado adversario arriscou. Quando o lado perdedor e
 * consumido por inteiro ({@code M == total_perdedor}), a formula coincide com o rateio
 * parimutuel classico {@code valor * pool / total_vencedor}.
 *
 * <h2>Casos de borda</h2>
 * <ul>
 *   <li><b>Sem apostas:</b> no-op.</li>
 *   <li><b>Lado vencedor vazio</b> ({@code total_W == 0}): {@code M == 0}, cada perdedor
 *       recebe {@code payout = valor} de volta (reembolso total).</li>
 *   <li><b>Lado perdedor vazio:</b> {@code M == 0}, cada vencedor recebe exatamente o que
 *       apostou (multiplicador 1x). Nenhum tratamento especial.</li>
 * </ul>
 *
 * <h2>Residuo de centavos (invariante critica)</h2>
 * A divisao gera dizimas. Arredondar cada payout isoladamente faria a soma nao bater
 * com o pool (centavos criados/destruidos). Usamos o <b>metodo do maior resto</b>
 * (largest remainder) sobre <i>todas</i> as apostas com payout: trunca cada payout para
 * baixo e distribui os centavos que sobraram, um a um, aos maiores restos fracionarios.
 * Garante deterministicamente {@code soma(payouts) == pool} ao centavo.
 */
public final class SettlementCalculator {

	/** Escala monetaria: centavos. */
	private static final int MONEY_SCALE = 2;

	/** Escala interna para a divisao antes do truncamento (precisao de ranking do resto). */
	private static final int CALC_SCALE = 12;

	/** Um centavo. */
	private static final BigDecimal CENT = new BigDecimal("0.01");

	private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE);

	private SettlementCalculator() {
		// utilitario estatico
	}

	/**
	 * Calcula e grava o {@code payout} de cada aposta para o lado vencedor dado.
	 * Vencedores recebem o valor casado do lado perdedor alem da propria aposta;
	 * perdedores recebem de volta a parte da aposta que nao foi casada.
	 *
	 * @param bets        todas as apostas do mercado (mutadas in-place: seta {@code payout})
	 * @param winningSide lado vencedor (A ou B), obrigatorio
	 */
	public static void settle(List<Bet> bets, Side winningSide) {
		Objects.requireNonNull(winningSide, "winningSide");
		Objects.requireNonNull(bets, "bets");

		if (bets.isEmpty()) {
			// Mercado sem apostas: nada a fazer.
			return;
		}

		BigDecimal pool = ZERO_MONEY;
		BigDecimal totalWinner = ZERO_MONEY;
		BigDecimal totalLoser = ZERO_MONEY;
		for (Bet bet : bets) {
			pool = pool.add(bet.getValor());
			if (bet.getOpcao() == winningSide) {
				totalWinner = totalWinner.add(bet.getValor());
			} else {
				totalLoser = totalLoser.add(bet.getValor());
			}
		}

		// Valor casado: o maximo que cada lado pode ganhar/perder.
		BigDecimal matched = totalWinner.min(totalLoser);

		// Payout exato de cada aposta, com alta precisao; floor + maior resto no final.
		List<Allotment> allotments = new ArrayList<>(bets.size());
		BigDecimal sumFloor = ZERO_MONEY;
		for (Bet bet : bets) {
			boolean won = bet.getOpcao() == winningSide;
			BigDecimal total = won ? totalWinner : totalLoser;
			// Parte casada da aposta: valor * M / total_do_lado (0 se o lado esta vazio).
			BigDecimal matchedShare = total.signum() == 0
					? ZERO_MONEY.setScale(CALC_SCALE)
					: bet.getValor().multiply(matched).divide(total, CALC_SCALE, RoundingMode.HALF_UP);
			BigDecimal exact = won
					? bet.getValor().add(matchedShare)
					: bet.getValor().subtract(matchedShare);

			BigDecimal floor = exact.setScale(MONEY_SCALE, RoundingMode.DOWN);
			bet.setPayout(floor);
			sumFloor = sumFloor.add(floor);
			allotments.add(new Allotment(bet, exact.subtract(floor)));
		}

		// Centavos que sobraram apos o truncamento (0 <= residuo < numApostas).
		BigDecimal residual = pool.subtract(sumFloor);
		int extraCents = residual.movePointRight(MONEY_SCALE).intValueExact();

		// Distribui um centavo por vez aos maiores restos (desempate: maior aposta, depois id).
		allotments.sort(REMAINDER_DESC);
		for (int i = 0; i < extraCents && i < allotments.size(); i++) {
			Bet bet = allotments.get(i).bet;
			bet.setPayout(bet.getPayout().add(CENT));
		}
	}

	/** Par (aposta, parte fracionaria descartada no truncamento). */
	private static final class Allotment {
		final Bet bet;
		final BigDecimal remainder;

		Allotment(Bet bet, BigDecimal remainder) {
			this.bet = bet;
			this.remainder = remainder;
		}
	}

	/**
	 * Ordena as apostas para receber o centavo residual: maior resto primeiro; empate
	 * decidido pela maior aposta e, por fim, pelo menor id (nulls por ultimo) — deixando
	 * a distribuicao deterministica.
	 */
	private static final Comparator<Allotment> REMAINDER_DESC =
			Comparator.comparing((Allotment a) -> a.remainder).reversed()
					.thenComparing(Comparator.comparing((Allotment a) -> a.bet.getValor()).reversed())
					.thenComparing(a -> a.bet.getId(), Comparator.nullsLast(Comparator.naturalOrder()));
}
