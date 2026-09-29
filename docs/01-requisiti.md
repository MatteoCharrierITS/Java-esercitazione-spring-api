# Requisiti e perimetro

## Scenario

Un piccolo negozio vuole consultare il proprio catalogo e tenere sotto controllo
le scorte. Il sistema è diviso in un servizio REST che possiede i dati e in una
applicazione web che li visualizza. Solo il Producer può accedere al database.

## Requisiti funzionali

### Catalogo

- Creare, consultare, modificare e disattivare un prodotto.
- Associare ogni prodotto a una categoria esistente.
- Identificare il prodotto con uno **SKU univoco**, oltre all'id tecnico.
- Cercare per nome o SKU, senza distinzione tra maiuscole e minuscole.
- Filtrare per categoria, stato attivo e stato di scorta.
- Ordinare e paginare i risultati.
- Consultare l'elenco delle categorie attive.

### Magazzino

- Registrare un carico, uno scarico o una rettifica di inventario.
- Aggiornare quantità e storico in un'unica transazione.
- Impedire che uno scarico porti la quantità sotto zero.
- Considerare “sotto scorta” un prodotto la cui quantità è minore o uguale alla
  sua `sogliaScorta`.
- Consultare gli ultimi movimenti di un prodotto.

### Dashboard

La Consumer deve mostrare:

- numero di prodotti attivi;
- numero di prodotti sotto scorta;
- quantità totale disponibile;
- valore totale del magazzino, calcolato come somma di `prezzo * quantita`;
- elenco sintetico dei prodotti da riordinare.

### Inventory Pulse — control room live

- Calcolare un indice di salute del magazzino da 0 a 100.
- Assegnare a ogni prodotto una priorità `CRITICA`, `ALTA`, `MEDIA` oppure `OK`.
- Mostrare gli ultimi movimenti in un feed che si aggiorna senza ricaricare la
  pagina.
- Aggiornare KPI e radar delle categorie quando viene registrato un movimento.
- Segnalare chiaramente quando il collegamento live è interrotto e continuare a
  funzionare tramite aggiornamento periodico.

### Interfaccia web

- Elenco prodotti con ricerca, filtri, ordinamento e paginazione.
- Dettaglio prodotto con stato scorte e ultimi movimenti.
- Dashboard riepilogativa.
- Messaggi comprensibili per errori di validazione, dati mancanti e Producer non
  raggiungibile.

## Regole di business

1. `sku` è obbligatorio, univoco e scritto in maiuscolo; formato consigliato:
   `[A-Z0-9-]{3,30}`.
2. `nome` è obbligatorio e lungo al massimo 200 caratteri.
3. `prezzo` deve essere maggiore di zero e usa `BigDecimal`.
4. `quantita` e `sogliaScorta` non possono essere negative.
5. Una categoria disattivata non può essere assegnata a nuovi prodotti.
6. Il client non modifica direttamente la quantità tramite `PUT`: ogni variazione
   passa dall'endpoint movimenti per conservare lo storico.
7. La disattivazione di un prodotto non cancella i movimenti già registrati.
8. Due aggiornamenti concorrenti dello stesso prodotto non devono sovrascriversi
   silenziosamente: il secondo riceve un conflitto `409`.

## Requisiti non funzionali

- Java 21 e Spring Boot 3.
- Risposte JSON in UTF-8 e date ISO 8601.
- Validazione con Jakarta Bean Validation.
- Errori in formato coerente `application/problem+json`.
- Query di elenco paginata; dimensione predefinita 10, massima 100.
- URL del Producer configurabile nella Consumer.
- Migrazioni del database versionate; consigliato Flyway. `ddl-auto=validate` nei
  profili non di test.
- Log senza password, stack trace o dati sensibili nelle risposte HTTP.

## Funzionalità obbligatorie e bonus

### Obbligatorie

- CRUD logico prodotti, categorie in lettura, filtri e paginazione.
- Movimenti di scorta con storico.
- Dashboard e pagine Thymeleaf di elenco/dettaglio.
- Control room Inventory Pulse con aggiornamento SSE.
- Gestione centralizzata degli errori.

### Bonus facoltativi

- Cache breve della dashboard nella Consumer.
- Esportazione CSV dell'elenco filtrato.
- Test di integrazione con Testcontainers per PostgreSQL.
