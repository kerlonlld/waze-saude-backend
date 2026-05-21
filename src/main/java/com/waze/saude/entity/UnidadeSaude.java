package com.waze.saude.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "unidades_saude")
public class UnidadeSaude {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idUnidade; // Mudamos de 'id' para 'idUnidade' para casar com a Ficha

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String endereco;

    private int capacidadeMaxima;

    private Double latitude;
    private Double longitude;

    // --- GETTERS E SETTERS ---

    public Long getIdUnidade() { return idUnidade; }
    public void setIdUnidade(Long idUnidade) { this.idUnidade = idUnidade; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public void setCapacidadeMaxima(int capacidadeMaxima) {
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    // Método temporário/coringa para evitar erros de compilação em códigos antigos
    public Long getId() {
        return this.idUnidade;
    }
}
