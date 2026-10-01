package it.esercitazione.liveauction.producer.portafoglio.model;

import it.esercitazione.liveauction.producer.auth.models.Utente;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false, unique = true)
    private Utente utente;

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
