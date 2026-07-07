package com.davi.poolbet.repository;

import java.util.List;

import com.davi.poolbet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByGrupoIdAndNome(Long grupoId, String nome);

	List<User> findByGrupoIdOrderByNomeAsc(Long grupoId);
}
