package it.esercitazione.liveauction.producer.auth.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "utenti")
@Getter @Setter
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 200)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "ruolo", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Ruolo ruolo;

    @Column(name = "attivo", nullable = false)
    private boolean attivo;

    @Column(name = "data_creazione", nullable = false)
    @CreationTimestamp
    private Instant dataCreazione;

}
