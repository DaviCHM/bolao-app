package com.davi.poolbet.repository;

import java.util.List;
import java.util.Optional;

import com.davi.poolbet.model.Market;
import com.davi.poolbet.model.MarketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRepository extends JpaRepository<Market, Long> {

	List<Market> findByStatus(MarketStatus status);

	/**
	 * Carrega o mercado adquirindo lock pessimista de escrita (SELECT ... FOR UPDATE)
	 * na linha. E o ponto unico de serializacao: tanto a aposta quanto a resolucao
	 * passam por aqui, garantindo que nao ocorram concorrentemente sobre o mesmo mercado.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Market m where m.id = :id")
	Optional<Market> findByIdForUpdate(@Param("id") Long id);
}
