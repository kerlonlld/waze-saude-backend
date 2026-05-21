package com.waze.saude.service;

import com.waze.saude.entity.FichaAtendimento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class UbsService {

    // Simulação de Banco de Dados em Memória (Thread-safe)
    private final List<UbsFake> bancoUbs = new ArrayList<>();
    private final List<FichaAtendimento> bancoFichas = new CopyOnWriteArrayList<>();
    private final AtomicLong fichaIdGerador = new AtomicLong(1);

    public UbsService() {
        // Inicializa algumas unidades de saúde fictícias para testes (Exemplo: São Paulo)
        bancoUbs.add(new UbsFake(1L, "UBS Central", "Av. Paulista, 1000 - Bela Vista", -23.5615, -46.6562));
        bancoUbs.add(new UbsFake(2L, "UBS Vila Mariana", "Rua Domingos de Morais, 2500", -23.5982, -46.6369));
        bancoUbs.add(new UbsFake(3L, "UBS Consolação", "Rua Augusta, 400 - Consolação", -23.5489, -46.6494));
    }

    // DTO interno exigido pelo UbsController
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UbsDTO {
        private Long idUnidade; // Mapeia dinamicamente idUnidade ou id para o frontend Vue
        private Long id;
        private String nome;
        private String endereco;
        private Double distancia;
        private Integer ocupacaoAtual;
        private Integer tempoEspera;
        private Double taxaOcupacao;
    }

    // Classe auxiliar para simular a tabela de UBS
    @Data
    @AllArgsConstructor
    private static class UbsFake {
        private Long id;
        private String nome;
        private String endereco;
        private Double latitude;
        private Double longitude;
    }

    /**
     * 1. Buscar unidades (Retorna TODAS as UBSs ordenadas pela proximidade, sem limite de raio)
     */
    public List<UbsDTO> buscarUbsProximas(Double latCliente, Double lonCliente, double raioKm) {
        return bancoUbs.stream()
                .map(ubs -> {
                    double dist = calcularDistanciaHaversine(latCliente, lonCliente, ubs.getLatitude(), ubs.getLongitude());

                    // Conta quantas fichas estão AGUARDANDO nesta unidade
                    int ocupacao = (int) bancoFichas.stream()
                            .filter(f -> f.getUnidadeSaude() != null &&
                                    ubs.getId().equals(f.getUnidadeSaude().getIdUnidade()) &&
                                    "AGUARDANDO".equals(f.getStatus()))
                            .count();

                    // Regra de negócio simples para tempo e taxa de ocupação
                    int tempoEstimadoPorPessoa = 12; // minutos
                    int tempoEspera = ocupacao * tempoEstimadoPorPessoa;
                    double taxaOcupacao = Math.min((double) ocupacao / 15.0, 1.0); // 15 pessoas vira 100% (1.0)

                    return new UbsDTO(ubs.getId(), ubs.getId(), ubs.getNome(), ubs.getEndereco(), dist, ocupacao, tempoEspera, taxaOcupacao);
                })
                .sorted((u1, u2) -> Double.compare(u1.getDistancia(), u2.getDistancia()))
                .collect(Collectors.toList());
    }

    /**
     * 2. Registrar Check-in (Entrar na fila com CPF e Nome)
     */
    public FichaAtendimento registrarCheckIn(String cpf, String nome, Long unidadeId) {
        // Validação básica se já está na fila
        FichaAtendimento existente = buscarFichaAtivaPorCpf(cpf);
        if (existente != null) {
            throw new RuntimeException("O paciente com este CPF já possui um check-in ativo em andamento.");
        }

        UbsFake ubsAlvo = bancoUbs.stream()
                .filter(u -> u.getId().equals(unidadeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unidade de saúde não encontrada."));

        // Monta a estrutura da Ficha simulando a Entity externa
        FichaAtendimento novaFicha = new FichaAtendimento();
        novaFicha.setId(fichaIdGerador.getAndIncrement());
        novaFicha.setStatus("AGUARDANDO");
        novaFicha.setTempoEsperaMinutos(0);
        novaFicha.setEntradaFila(LocalDateTime.now());

        // ATUALIZADO: Agora salvando o CPF e o Nome digitado na nova estrutura da classe PacienteMock
        FichaAtendimento.PacienteMock paciente = new FichaAtendimento.PacienteMock();
        paciente.setCpf(cpf);
        paciente.setNomeCompleto(nome);

        novaFicha.setPaciente(paciente);
        novaFicha.setUnidadeSaude(new FichaAtendimento.UnidadeMock(ubsAlvo.getId(), ubsAlvo.getNome(), ubsAlvo.getEndereco()));

        bancoFichas.add(novaFicha);
        return novaFicha;
    }

    /**
     * 3. Buscar Fila Ativa da Unidade (Usado pelo Polling do frontend)
     */
    public List<FichaAtendimento> buscarFichaPorUnidadeId(Long unidadeId) {
        return bancoFichas.stream()
                .filter(f -> f.getUnidadeSaude() != null &&
                        unidadeId.equals(f.getUnidadeSaude().getIdUnidade()) &&
                        ("AGUARDANDO".equals(f.getStatus()) || "EM_ATENDIMENTO".equals(f.getStatus())))
                .collect(Collectors.toList());
    }

    /**
     * 4. Buscar ficha ativa do paciente pelo CPF
     */
    public FichaAtendimento buscarFichaAtivaPorCpf(String cpf) {
        if (cpf == null) return null;
        final String cpfLimpo = cpf.replaceAll("\\D", "");

        return bancoFichas.stream()
                .filter(f -> f.getPaciente() != null && f.getPaciente().getCpf() != null &&
                        cpfLimpo.equals(f.getPaciente().getCpf().replaceAll("\\D", "")) &&
                        ("AGUARDANDO".equals(f.getStatus()) || "EM_ATENDIMENTO".equals(f.getStatus())))
                .findFirst()
                .orElse(null);
    }

    /**
     * 5. Cancelar Check-in / Sair da Fila (Check-out)
     */
    public void cancelarCheckIn(Long fichaId) {
        FichaAtendimento ficha = bancoFichas.stream()
                .filter(f -> f.getId().equals(fichaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Ticket/Ficha de atendimento não encontrada."));

        ficha.setStatus("CANCELADO");
    }

    /**
     * 6. Chamar próximo paciente (Painel de Atendimento)
     */
    public FichaAtendimento chamarProximo(Long unidadeId) {
        // Altera o status de quem estava "EM_ATENDIMENTO" na unidade anterior para "FINALIZADO"
        bancoFichas.stream()
                .filter(f -> f.getUnidadeSaude() != null &&
                        unidadeId.equals(f.getUnidadeSaude().getIdUnidade()) &&
                        "EM_ATENDIMENTO".equals(f.getStatus()))
                .forEach(f -> f.setStatus("FINALIZADO"));

        // Seleciona o primeiro da fila que está "AGUARDANDO"
        FichaAtendimento proximo = bancoFichas.stream()
                .filter(f -> f.getUnidadeSaude() != null &&
                        unidadeId.equals(f.getUnidadeSaude().getIdUnidade()) &&
                        "AGUARDANDO".equals(f.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Não há mais pacientes aguardando nesta fila."));

        proximo.setStatus("EM_ATENDIMENTO");
        return proximo;
    }

    /**
     * Método Geográfico Auxiliar: Calcula distância em km entre dois pontos geográficos
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Raio da Terra em quilômetros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}