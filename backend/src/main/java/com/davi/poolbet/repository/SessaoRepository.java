package com.davi.poolbet.repository;

import com.davi.poolbet.model.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, String> {
}
