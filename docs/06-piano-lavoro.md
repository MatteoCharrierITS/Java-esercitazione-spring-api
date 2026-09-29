# Piano di lavoro e criteri di accettazione

Questo documento definisce l'ordine consigliato di implementazione, senza
prescrivere i dettagli del codice.

## Fase 1 — Fondamenta Producer

- Creare il modulo Spring Boot, configurare PostgreSQL e le migrazioni.
- Modellare categorie e prodotti con DTO separati.
- Implementare CRUD logico, validazione e gestione errori.
- Aggiungere ricerca combinata, ordinamento e paginazione.

Completata quando:

- lo schema nasce da zero tramite migrazioni;
- gli endpoint prodotto rispettano status e payload documentati;
- filtri combinati e paginazione restituiscono risultati coerenti;
- nessuna entity JPA è serializzata direttamente.

## Fase 2 — Magazzino e dashboard

- Implementare movimento e aggiornamento atomico della giacenza.
- Gestire scorta insufficiente e aggiornamenti concorrenti.
- Implementare query aggregate della dashboard.

Completata quando:

- un carico/scarico modifica la quantità e crea un solo movimento;
- in caso di errore non viene salvata nessuna modifica parziale;
- uno scarico oltre disponibilità restituisce `409`;
- KPI e prodotti sotto soglia riflettono i dati correnti.

## Fase 3 — Consumer

- Configurare `RestClient` e DTO client.
- Realizzare dashboard, elenco, dettaglio e navigazione.
- Gestire filtri, pagine e indisponibilità del Producer.

Completata quando:

- la Consumer funziona senza dipendenze JPA/PostgreSQL;
- ogni dato visualizzato proviene dalle API;
- spegnendo il Producer appare una pagina di errore controllata;
- la navigazione conserva i filtri applicati.

## Fase 4 — Inventory Pulse

- Calcolare salute, priorità e suggerimento di riordino nel Producer.
- Pubblicare un evento SSE soltanto dopo il commit di un movimento.
- Realizzare il relay SSE nella Consumer e la control room responsive.
- Aggiungere riconnessione automatica e polling di sicurezza.

Completata quando:

- aprendo la control room compare lo stato `LIVE`;
- una chiamata all'endpoint movimenti aggiorna KPI e feed senza refresh manuale;
- interrompendo lo stream compare `RICONNESSIONE…` senza rompere la pagina;
- la pagina torna coerente anche dopo aver perso uno o più eventi.

## Matrice minima di test

| Area | Scenario | Esito atteso |
| --- | --- | --- |
| Creazione | SKU nuovo e dati validi | `201`, location e dettaglio |
| Creazione | SKU duplicato | `409` |
| Validazione | prezzo zero o quantità negativa | `400` |
| Ricerca | nome + categoria + sort | risultati filtrati e ordinati |
| Paginazione | pagina oltre l'ultima | `200`, content vuoto |
| Dettaglio | id inesistente | `404` |
| Update | versione superata | `409` |
| Delete | prodotto esistente o già inattivo | `204` |
| Magazzino | carico di 5 | quantità +5 e movimento creato |
| Magazzino | scarico oltre giacenza | `409`, nessuna modifica |
| Magazzino | rettifica a zero | stato `ESAURITO` |
| Dashboard | prodotto passa sotto soglia | contatore aggiornato |
| Inventory Pulse | nuovo movimento | feed e KPI aggiornati live |
| Inventory Pulse | stream interrotto | stato offline e fallback polling |
| Consumer | Producer spento | pagina servizio non disponibile |

## Definition of Done

La consegna è completa quando:

- Producer e Consumer si avviano separatamente sulle porte previste;
- PostgreSQL è accessibile soltanto dal Producer;
- contratto API, schema database e comportamento reale coincidono;
- sono presenti dati demo significativi;
- i test coprono almeno service, controller REST e client HTTP;
- i README dei due moduli spiegano configurazione e avvio;
- password e file locali non sono versionati.

## Scelte lasciate allo sviluppatore

- uso di Specification JPA, query JPQL o metodi repository per i filtri;
- Bootstrap o CSS proprietario per Thymeleaf;
- MapStruct o mapping manuale dei DTO;
- cache Consumer e test con Testcontainers, entrambi bonus.
