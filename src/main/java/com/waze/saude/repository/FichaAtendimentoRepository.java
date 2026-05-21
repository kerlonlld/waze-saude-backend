package com.waze.saude.repository;

import com.waze.saude.entity.FichaAtendimento;
import com.waze.saude.entity.Paciente;
import com.waze.saude.entity.UnidadeSaude;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FichaAtendimentoRepository extends JpaRepository<FichaAtendimento, Long> {

    // 1. Verifica se o paciente já está na fila de uma determinada UBS
    boolean existsByPacienteAndUnidadeSaudeAndStatus(Paciente paciente, UnidadeSaude unidadeSaude, String status);

    // 2. Busca o histórico de fichas por ID da Unidade (Mantido o seu original)
    List<FichaAtendimento> findByUnidadeSaude_IdUnidadeOrderByEntradaFilaAsc(Long unidadeId);

    // 3. ADICIONADO: Busca fichas ativas do paciente pelo CPF (Para a rota /atendimento/ativa)
    List<FichaAtendimento> findByPacienteCpfAndStatusIn(String cpf, List<String> statuses);

    // 4. ADICIONADO: Busca a fila atual (Aguardando/Em Atendimento) de uma UBS por ID ordenando pela ordem de chegada
    List<FichaAtendimento> findByUnidadeSaude_IdUnidadeAndStatusInOrderByEntradaFilaAsc(Long idUnidade, List<String> statuses);
}