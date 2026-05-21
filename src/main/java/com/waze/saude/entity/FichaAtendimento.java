package com.waze.saude.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_ficha_atendimento")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FichaAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status; // "AGUARDANDO", "EM_ATENDIMENTO", "CANCELADO", "FINALIZADO"

    private Integer tempoEsperaMinutos;

    // Campo de data/hora exigido pela ordenação do seu Repository
    private LocalDateTime entradaFila = LocalDateTime.now();

    @Embedded
    private PacienteMock paciente;

    @Embedded
    private UnidadeMock unidadeSaude;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PacienteMock {
        @Column(name = "paciente_cpf")
        private String cpf;

        // NOVO CAMPO: Adicionado para receber o nome vindo do Vue e Painel
        @Column(name = "paciente_nome")
        private String nomeCompleto;

        // Construtor customizado para manter compatibilidade com buscas rápidas por CPF
        public PacienteMock(String cpf) {
            this.cpf = cpf;
        }
    }

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UnidadeMock {
        // Mudamos o nome de 'id' para 'idUnidade' para casar com o seu FichaAtendimentoRepository
        @Column(name = "unidade_id")
        private Long idUnidade;

        @Column(name = "unidade_nome")
        private String nome;

        @Column(name = "unidade_endereco")
        private String endereco;
    }
}