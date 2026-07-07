package com.davi.poolbet.repository;

import java.util.Optional;

import com.davi.poolbet.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

	Optional<Grupo> findByNome(String nome);

	boolean existsByNome(String nome);
}
