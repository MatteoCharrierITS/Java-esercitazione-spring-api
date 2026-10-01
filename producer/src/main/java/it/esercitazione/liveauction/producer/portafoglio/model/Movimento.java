package it.esercitazione.liveauction.producer.portafoglio.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importo;

    @Column(length = 255)
    private String descrizione;

    @Column(name = "data_creazione", nullable = false)
    private LocalDateTime dataCreazione;

    @Column(name = "offerta_id")
    private Long offertaId;

    @PrePersist
    protected void primaDelSalvataggio() {
        if (dataCreazione == null) {
            dataCreazione = LocalDateTime.now();
        }
    }

}
