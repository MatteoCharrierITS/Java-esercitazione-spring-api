# Catalogo & Magazzino — Spring Boot Producer/Consumer

Progetto didattico basato su due applicazioni Spring Boot indipendenti:

- **product-api** espone API REST, applica le regole di business e persiste i dati su PostgreSQL;
- **product-client** consuma esclusivamente le API e presenta i dati tramite Thymeleaf.

Il progetto parte da un CRUD di prodotti e lo estende in un piccolo sistema di magazzino: categorie, SKU, movimenti di carico e scarico, soglie di riordino, ricerca paginata e una control room live chiamata **Inventory Pulse**.

> **Stato del progetto:** progettazione completata e scheletri Spring Boot creati. I moduli compilano e i test iniziali passano; il dominio applicativo è ancora da implementare.

## La feature distintiva: Inventory Pulse

Inventory Pulse è una dashboard operativa aggiornata in tempo reale tramite Server-Sent Events. Quando viene registrato un movimento di magazzino, la pagina reagisce senza refresh manuale mostrando:

- indice di salute del magazzino da 0 a 100;
- prodotti con priorità `CRITICA`, `ALTA`, `MEDIA` oppure `OK`;
- valore e quantità complessivi;
- radar dello stato delle categorie;
- feed live degli ultimi movimenti;
- quantità suggerita per il riordino.

Se lo stream live si interrompe, la UI mostra lo stato di riconnessione e usa un polling periodico come fallback.

## Architettura

```text
┌─────────────┐      HTML       ┌─────────────────────┐
│   Browser   │ ◄─────────────► │ product-client      │
│             │      SSE        │ Spring + Thymeleaf  │
└─────────────┘                 │ :8082               │
                                └──────────┬──────────┘
                                           │ REST / JSON / SSE
                                ┌──────────▼──────────┐
                                │ product-api         │
                                │ Spring Data JPA     │
                                │ :8081               │
                                └──────────┬──────────┘
                                           │
                                ┌──────────▼──────────┐
                                │ PostgreSQL          │
                                │ esercitazione_api   │
                                │ :5432               │
                                └─────────────────────┘
```

La Consumer non possiede repository, entity JPA o credenziali del database. Il Producer è l'unico proprietario dei dati.

## Tecnologie previste

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA e Hibernate
- Jakarta Bean Validation
- PostgreSQL
- Flyway
- Thymeleaf
- `RestClient` per le normali richieste HTTP
- `WebClient` per il flusso SSE
- Lombok
- JUnit 5 e Mockito
- Testcontainers PostgreSQL, facoltativo
- Docker Compose per PostgreSQL locale
- immagini Docker multi-stage e profilo Compose `prod`

## Funzionalità

### Producer

- CRUD logico dei prodotti con DTO separati dalle entity;
- SKU univoco e categorie normalizzate;
- filtri combinabili, ordinamento e paginazione;
- movimenti di carico, scarico e rettifica;
- aggiornamento atomico di quantità e storico;
- protezione da scorte negative;
- optimistic locking sugli aggiornamenti concorrenti;
- dashboard aggregata di magazzino;
- stream SSE degli aggiornamenti di scorta;
- errori coerenti in formato `application/problem+json`.

### Consumer

- dashboard riepilogativa;
- control room Inventory Pulse;
- tabella prodotti ricercabile e paginata;
- filtri per categoria e stato scorte;
- pagina di dettaglio con storico movimenti;
- gestione del Producer non disponibile;
- relay SSE verso il browser con fallback polling.

## Modello dati

Il database contiene tre tabelle principali:

| Tabella | Responsabilità |
| --- | --- |
| `categorie` | classificazione normalizzata dei prodotti |
| `prodotti` | anagrafica, prezzo, giacenza e soglia di riordino |
| `movimenti_magazzino` | storico immutabile di carichi, scarichi e rettifiche |

La specifica completa, con relazioni, vincoli, indici e DDL PostgreSQL, è in [docs/03-database.md](docs/03-database.md).

## API principali

Base URL prevista: `http://localhost:8081/api/v1`

| Metodo | Endpoint | Descrizione |
| --- | --- | --- |
| `GET` | `/prodotti` | ricerca paginata e filtri |
| `POST` | `/prodotti` | creazione prodotto |
| `GET` | `/prodotti/{id}` | dettaglio prodotto |
| `PUT` | `/prodotti/{id}` | aggiornamento anagrafico |
| `DELETE` | `/prodotti/{id}` | disattivazione logica |
| `POST` | `/prodotti/{id}/movimenti` | carico, scarico o rettifica |
| `GET` | `/prodotti/{id}/movimenti` | storico movimenti |
| `GET` | `/categorie` | categorie disponibili |
| `GET` | `/dashboard` | indicatori sintetici |
| `GET` | `/dashboard/control-room` | snapshot Inventory Pulse |
| `GET` | `/events/scorte` | stream Server-Sent Events |

Request, response, parametri e codici di errore sono documentati in [docs/04-api-rest.md](docs/04-api-rest.md).

## Struttura prevista

```text
.
├── producer/
│   ├── pom.xml
│   └── src/
├── consumer/
│   ├── pom.xml
│   └── src/
├── docs/
├── compose.yaml
├── .env.example
├── Consegna.md
└── README.md
```

## Requisiti per l'esecuzione

- JDK 21
- Maven 3.9 o Maven Wrapper
- Docker Desktop con Compose, soluzione consigliata;
- in alternativa PostgreSQL 16 o versione compatibile.

### PostgreSQL con Docker

Il Producer include `spring-boot-docker-compose`: quando viene avviato dalla
directory `producer`, Spring Boot legge `../compose.yaml`, crea PostgreSQL e
attende che l'healthcheck sia positivo.

Per avviare il solo database manualmente:

```powershell
docker compose up -d postgres
docker compose ps
```

I valori predefiniti sono adatti allo sviluppo locale:

| Variabile | Default |
| --- | --- |
| `POSTGRES_DB` | `esercitazione_api` |
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | `postgres` |
| `POSTGRES_PORT` | `5432` |

Per personalizzarli, copiare `.env.example` in `.env`. Il file `.env` è escluso
da Git. I dati sono persistiti nel volume Docker
`catalogo-magazzino-postgres-data`.

Se la porta `5432` è già occupata, impostare `POSTGRES_PORT=5433` nel file
`.env`: la service connection di Spring userà automaticamente la porta esposta.

Per usare invece un PostgreSQL installato localmente, disabilitare l'integrazione
Docker con `DOCKER_COMPOSE_ENABLED=false` e creare il database tramite `psql`:

```sql
CREATE DATABASE esercitazione_api
WITH ENCODING 'UTF8';
```

### Intero stack Docker — profilo `prod`

Il profilo `prod` costruisce immagini Java 21 separate e avvia i servizi in
ordine, aspettando l'healthcheck di ogni dipendenza:

```text
PostgreSQL healthy → Producer healthy → Consumer healthy
```

Avvio completo:

```powershell
docker compose --profile prod up --build -d
docker compose --profile prod ps
```

La Consumer sarà disponibile su `http://localhost:8082`; le API e il relativo
healthcheck su `http://localhost:8081` e `/actuator/health`.

Log e arresto:

```powershell
docker compose --profile prod logs -f
docker compose --profile prod down
```

`down` conserva il volume PostgreSQL. Per eliminare deliberatamente anche i dati
locali usare `docker compose --profile prod down --volumes`.

Nei container viene attivato il profilo Spring `prod`, l'integrazione Docker
Compose interna al Producer viene disabilitata e la Consumer contatta il
Producer tramite il nome DNS `producer`. Le immagini usano un utente non root,
filesystem in sola lettura e una directory `/tmp` temporanea.

Le tabelle saranno create dalle migrazioni Flyway del Producer. Lo schema di riferimento è già disponibile nella documentazione, ma non è ancora presente come migrazione eseguibile perché il dominio non è stato implementato.

## Configurazione prevista

Producer, `producer/src/main/resources/application.properties`:

```properties
spring.application.name=product-api
server.port=8081

spring.datasource.url=jdbc:postgresql://localhost:5432/esercitazione_api
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

Consumer, `consumer/src/main/resources/application.properties`:

```properties
spring.application.name=product-client
server.port=8082

product-api.base-url=${PRODUCT_API_URL:http://localhost:8081/api/v1}
product-api.connect-timeout=2s
product-api.read-timeout=5s
```

Password e configurazioni locali non devono essere versionate.

## Build e avvio

Per compilare e testare entrambi i moduli dalla radice, senza un'installazione globale di Maven:

```powershell
.\producer\mvnw.cmd -f pom.xml test
```

Per avviare le applicazioni:

1. verificare che Docker Desktop sia attivo;
2. avviare il Producer dalla sua directory: PostgreSQL partirà automaticamente;
3. avviare la Consumer in un secondo terminale;
4. aprire la dashboard nel browser.

```powershell
Set-Location producer
.\mvnw.cmd spring-boot:run
```

In un secondo terminale:

```powershell
Set-Location consumer
.\mvnw.cmd spring-boot:run
```

Pagine previste:

- `http://localhost:8082/dashboard`
- `http://localhost:8082/control-room`
- `http://localhost:8082/prodotti`

Arrestando normalmente il Producer, Spring Boot ferma il container senza
rimuovere il volume. La Consumer può essere avviata indipendentemente, ma
mostrerà dati soltanto quando le API saranno implementate e disponibili.

## Documentazione

- [Indice della progettazione](docs/README.md)
- [Requisiti e regole di business](docs/01-requisiti.md)
- [Architettura e componenti](docs/02-architettura.md)
- [Database e schema PostgreSQL](docs/03-database.md)
- [Contratto delle API REST](docs/04-api-rest.md)
- [Interfaccia web e flussi](docs/05-ui-flussi.md)
- [Piano di lavoro e criteri di accettazione](docs/06-piano-lavoro.md)
- [Inventory Pulse](docs/07-inventory-pulse.md)

La consegna estesa di riferimento resta disponibile in [Consegna.md](Consegna.md).
