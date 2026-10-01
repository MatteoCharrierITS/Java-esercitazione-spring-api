package it.esercitazione.liveauction.producer.portafoglio.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utente_id", nullable = false, unique = true)
    private Long utenteId;

    @Column(name = "saldo_totale", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoTotale;

    @Column(name = "saldo_riservato", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoRiservato;

    @Version
    private Long versione;

    @Transient
    public BigDecimal getSaldoDisponibile() {
        return saldoTotale.subtract(saldoRiservato);
    }

}
