# Documentazione di progettazione

Questa cartella descrive la versione estesa dell'esercitazione Spring Boot. La
consegna originale resta disponibile in [`../Consegna.md`](../Consegna.md); i
documenti qui presenti costituiscono la specifica da seguire per la nuova
versione.

## Obiettivo in breve

Realizzare due applicazioni indipendenti:

- **product-api** (`:8081`): catalogo prodotti e gestione delle scorte, con
  persistenza PostgreSQL e API REST;
- **product-client** (`:8082`): interfaccia Thymeleaf che usa esclusivamente le
  API del Producer.

L'estensione principale trasforma il semplice CRUD in un piccolo sistema di
magazzino. Ogni prodotto ha uno SKU e una soglia minima; carichi e scarichi
generano movimenti storicizzati. La feature distintiva è **Inventory Pulse**, una
control room che reagisce in tempo reale ai movimenti, mostra la salute del
magazzino e ordina gli articoli in base all'urgenza di riordino.

## Indice

1. [Requisiti e perimetro](01-requisiti.md)
2. [Architettura e componenti](02-architettura.md)
3. [Modello dati e schema SQL](03-database.md)
4. [Contratto delle API REST](04-api-rest.md)
5. [Interfaccia web e flussi](05-ui-flussi.md)
6. [Piano di lavoro e criteri di accettazione](06-piano-lavoro.md)
7. [Inventory Pulse — feature distintiva](07-inventory-pulse.md)

## Decisioni chiave

| Tema | Decisione |
| --- | --- |
| Contratto API | prefisso `/api/v1` e DTO separati dalle entity |
| Elenco prodotti | filtri combinabili, ordinamento e paginazione |
| Categoria | tabella dedicata, non stringa libera sul prodotto |
| Scorte | variazioni tramite endpoint dedicato e movimento atomico |
| Prodotto eliminato | eliminazione logica tramite campo `attivo` |
| Concorrenza | optimistic locking con campo `versione` |
| Consumer | nessun accesso a JPA/PostgreSQL; usa `RestClient` |
| Funzione distintiva | control room live “Inventory Pulse” tramite SSE |

## Fuori perimetro

Per mantenere l'esercizio affrontabile, non sono richiesti autenticazione,
utenti, ordini, pagamenti, upload immagini, code di messaggi o deployment cloud.
