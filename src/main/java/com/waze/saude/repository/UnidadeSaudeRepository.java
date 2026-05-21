package com.waze.saude.repository;

import com.waze.saude.entity.UnidadeSaude;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeSaudeRepository extends JpaRepository<UnidadeSaude, Long> {
}