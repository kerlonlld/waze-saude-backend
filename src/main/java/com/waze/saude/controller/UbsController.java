package com.waze.saude.controller;

import com.waze.saude.entity.FichaAtendimento;
import com.waze.saude.service.UbsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ubs")
@RequiredArgsConstructor
// CORREÇÃO: Garante que origens, métodos e cabeçalhos sejam aceitos sem restrições do navegador
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class UbsController {

    private final UbsService ubsService;

    // 1. Buscar próximas unidades
    @GetMapping("/proximas")
    public List<UbsService.UbsDTO> buscarProximas(@RequestParam Double lat, @RequestParam Double lon) {
        return ubsService.buscarUbsProximas(lat, lon, 50.0);
    }

    // 2. Fazer Check-in (Atualizado para capturar e enviar o nome)
    @PostMapping("/checkin")
    public ResponseEntity<?> fazerCheckIn(@RequestBody Map<String, Object> payload) {
        try {
            String cpf = (String) payload.get("cpf");
            // CORREÇÃO: Captura a nova chave "nome" enviada pelo payload do Vue
            String nome = (String) payload.get("nome");
            Long unidadeId = Long.valueOf(payload.get("unidadeId").toString());

            // CORREÇÃO: Passa o nome para o método atualizado do UbsService
            FichaAtendimento novaFicha = ubsService.registrarCheckIn(cpf, nome, unidadeId);
            return ResponseEntity.ok(novaFicha);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
//test

    // 3. Buscar Fila da Unidade por ID
    @GetMapping("/fila/{unidadeId}")
    public ResponseEntity<?> obterFilaPorUnidade(@PathVariable Long unidadeId) {
        try {
            List<FichaAtendimento> fila = ubsService.buscarFichaPorUnidadeId(unidadeId);
            return ResponseEntity.ok(fila);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 4. Buscar ficha ativa do paciente pelo CPF
    @GetMapping("/atendimento/ativa")
    public ResponseEntity<?> buscarFichaAtiva(@RequestParam String cpf) {
        try {
            FichaAtendimento ficha = ubsService.buscarFichaAtivaPorCpf(cpf);
            if (ficha != null) {
                return ResponseEntity.ok(ficha);
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 5. Cancelar Check-in / Sair da Fila (Check-out)
    @PostMapping("/atendimento/checkout/{fichaId}")
    public ResponseEntity<?> cancelarCheckIn(@PathVariable Long fichaId) {
        try {
            ubsService.cancelarCheckIn(fichaId);
            return ResponseEntity.ok(Map.of("message", "Check-in cancelado com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 6. Chamar próximo paciente (MÉTODO ADICIONADO PARA FUNCIONAR O PAINEL)
    @PostMapping("/atendimento/chamar/{unidadeId}")
    public ResponseEntity<?> chamarProximo(@PathVariable Long unidadeId) {
        try {
            FichaAtendimento fichaChamada = ubsService.chamarProximo(unidadeId);
            return ResponseEntity.ok(fichaChamada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}