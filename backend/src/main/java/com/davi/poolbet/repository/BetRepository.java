package com.davi.poolbet.repository;

import java.util.List;

import com.davi.poolbet.model.Bet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetRepository extends JpaRepository<Bet, Long> {

	List<Bet> findByMarketId(Long marketId);
}
