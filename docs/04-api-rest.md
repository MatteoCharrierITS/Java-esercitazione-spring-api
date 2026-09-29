# Contratto delle API REST

## Convenzioni

- Base URL: `http://localhost:8081/api/v1`
- Content type: `application/json`
- Date e orari: ISO 8601, ad esempio `2026-09-29T14:30:00`
- Importi: numero JSON con due decimali, gestito in Java con `BigDecimal`
- Gli elenchi paginati usano indici pagina a partire da zero.

## Riepilogo endpoint

| Metodo | Endpoint | Scopo |
| --- | --- | --- |
| `GET` | `/prodotti` | ricerca e lista paginata |
| `POST` | `/prodotti` | crea prodotto |
| `GET` | `/prodotti/{id}` | dettaglio prodotto |
| `PUT` | `/prodotti/{id}` | aggiorna dati anagrafici |
| `DELETE` | `/prodotti/{id}` | disattiva prodotto |
| `POST` | `/prodotti/{id}/movimenti` | registra variazione di scorta |
| `GET` | `/prodotti/{id}/movimenti` | storico movimenti paginato |
| `GET` | `/categorie` | categorie disponibili |
| `GET` | `/dashboard` | indicatori di magazzino |
| `GET` | `/dashboard/control-room` | vista aggregata Inventory Pulse |
| `GET` | `/events/scorte` | stream SSE delle variazioni |

## Prodotti

### Ricerca e lista

```http
GET /api/v1/prodotti?query=laptop&categoria=informatica&statoScorta=SOTTO_SOGLIA&attivo=true&page=0&size=10&sort=prezzo,desc
```

Parametri tutti facoltativi:

| Parametro | Valori | Default |
| --- | --- | --- |
| `query` | parte del nome o SKU | nessun filtro |
| `categoria` | slug categoria | tutte |
| `statoScorta` | `DISPONIBILE`, `SOTTO_SOGLIA`, `ESAURITO` | tutti |
| `attivo` | `true`, `false` | `true` |
| `page` | intero >= 0 | `0` |
| `size` | 1..100 | `10` |
| `sort` | `nome`, `prezzo`, `quantita`, `dataCreazione` + `asc/desc` | `nome,asc` |

Risposta `200 OK`:

```json
{
  "content": [
    {
      "id": 1,
      "sku": "INF-LAP-001",
      "nome": "Laptop Pro 15",
      "prezzo": 1299.90,
      "quantita": 3,
      "sogliaScorta": 5,
      "statoScorta": "SOTTO_SOGLIA",
      "categoria": {
        "id": 1,
        "nome": "Informatica",
        "slug": "informatica"
      },
      "attivo": true
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Creazione

```http
POST /api/v1/prodotti
Content-Type: application/json
```

```json
{
  "sku": "INF-NBG-001",
  "nome": "Notebook Gaming",
  "descrizione": "Notebook ad alte prestazioni",
  "prezzo": 1599.90,
  "categoriaId": 1,
  "quantitaIniziale": 8,
  "sogliaScorta": 3
}
```

Risposta: `201 Created`, header `Location: /api/v1/prodotti/21` e DTO di
dettaglio nel body. Uno SKU duplicato produce `409 Conflict`.

### Dettaglio

```http
GET /api/v1/prodotti/1
```

Risposta `200 OK`:

```json
{
  "id": 1,
  "sku": "INF-LAP-001",
  "nome": "Laptop Pro 15",
  "descrizione": "Notebook professionale...",
  "prezzo": 1299.90,
  "quantita": 3,
  "sogliaScorta": 5,
  "statoScorta": "SOTTO_SOGLIA",
  "categoria": {
    "id": 1,
    "nome": "Informatica",
    "slug": "informatica"
  },
  "attivo": true,
  "versione": 2,
  "dataCreazione": "2026-09-01T09:00:00",
  "dataModifica": "2026-09-29T14:30:00"
}
```

### Aggiornamento anagrafico

La quantità non è presente: si modifica solo tramite un movimento.

```http
PUT /api/v1/prodotti/1
Content-Type: application/json
```

```json
{
  "sku": "INF-LAP-001",
  "nome": "Laptop Pro 15 Gen 2",
  "descrizione": "Nuova generazione",
  "prezzo": 1399.90,
  "categoriaId": 1,
  "sogliaScorta": 4,
  "versione": 2
}
```

Risposta `200 OK`. Una versione non aggiornata produce `409 Conflict`.

### Disattivazione

```http
DELETE /api/v1/prodotti/1
```

Risposta `204 No Content`. Il record resta nel database con `attivo = false`.
Ripetere l'operazione mantiene comportamento idempotente e restituisce `204`.

## Movimenti di magazzino

### Registrazione

```http
POST /api/v1/prodotti/1/movimenti
Content-Type: application/json
```

Carico o scarico:

```json
{
  "tipo": "SCARICO",
  "quantita": 2,
  "nota": "Vendita banco 1042"
}
```

Per `RETTIFICA`, `quantita` indica la nuova giacenza assoluta. Risposta
`201 Created`:

```json
{
  "id": 35,
  "prodottoId": 1,
  "tipo": "SCARICO",
  "quantita": 2,
  "quantitaPrecedente": 5,
  "quantitaSuccessiva": 3,
  "nota": "Vendita banco 1042",
  "dataMovimento": "2026-09-29T14:30:00"
}
```

Uno scarico superiore alla giacenza produce `409 Conflict` con codice
`SCORTA_INSUFFICIENTE`.

### Storico

```http
GET /api/v1/prodotti/1/movimenti?page=0&size=10&sort=dataMovimento,desc
```

Risposta `200 OK` con la stessa busta paginata usata per i prodotti.

## Categorie

```http
GET /api/v1/categorie?attiva=true
```

```json
[
  { "id": 1, "nome": "Informatica", "slug": "informatica" },
  { "id": 2, "nome": "Accessori", "slug": "accessori" }
]
```

La gestione CRUD delle categorie non è richiesta: vengono inserite dalle
migrazioni iniziali.

## Dashboard

```http
GET /api/v1/dashboard
```

Risposta `200 OK`:

```json
{
  "prodottiAttivi": 20,
  "prodottiSottoScorta": 3,
  "quantitaTotale": 417,
  "valoreMagazzino": 84572.30,
  "daRiordinare": [
    {
      "id": 18,
      "sku": "UFF-SCR-001",
      "nome": "Scrivania Regolabile",
      "quantita": 2,
      "sogliaScorta": 5
    }
  ]
}
```

`daRiordinare` contiene al massimo 10 prodotti, ordinati per quantità crescente.

## Inventory Pulse

### Dati della control room

```http
GET /api/v1/dashboard/control-room?ultimiMovimenti=8
```

Risposta `200 OK`:

```json
{
  "saluteMagazzino": 74,
  "livelloSalute": "ATTENZIONE",
  "prodottiAttivi": 20,
  "prodottiCritici": 2,
  "valoreMagazzino": 84572.30,
  "categorie": [
    {
      "nome": "Informatica",
      "prodottiTotali": 5,
      "prodottiSottoScorta": 1,
      "salute": 80
    }
  ],
  "prioritaRiordino": [
    {
      "prodottoId": 18,
      "sku": "UFF-SCR-001",
      "nome": "Scrivania Regolabile",
      "quantita": 0,
      "sogliaScorta": 5,
      "priorita": "CRITICA",
      "pezziSuggeriti": 10
    }
  ],
  "ultimiMovimenti": [
    {
      "movimentoId": 35,
      "prodottoId": 1,
      "nomeProdotto": "Laptop Pro 15",
      "tipo": "SCARICO",
      "quantita": 2,
      "dataMovimento": "2026-09-29T14:30:00"
    }
  ],
  "generatoAlle": "2026-09-29T14:30:01"
}
```

Regole volutamente semplici e spiegabili:

- `CRITICA`: quantità uguale a zero;
- `ALTA`: quantità maggiore di zero ma non oltre metà soglia;
- `MEDIA`: quantità sopra metà soglia ma minore o uguale alla soglia;
- `OK`: quantità superiore alla soglia;
- `pezziSuggeriti`: quantità necessaria per arrivare a due volte la soglia;
- salute di un prodotto: `min(quantita / max(sogliaScorta, 1), 1) * 100`;
- salute del magazzino: media arrotondata della salute dei prodotti attivi.

Il livello generale è `CRITICO` tra 0 e 39, `ATTENZIONE` tra 40 e 74 e
`SALUTEVOLE` tra 75 e 100.

### Stream eventi

```http
GET /api/v1/events/scorte
Accept: text/event-stream
```

Esempio di evento emesso **dopo il commit** di un movimento:

```text
id: 35
event: scorta-aggiornata
data: {"prodottoId":1,"sku":"INF-LAP-001","tipo":"SCARICO","quantitaPrecedente":5,"quantitaSuccessiva":3,"priorita":"MEDIA","timestamp":"2026-09-29T14:30:00"}
```

Lo stream invia inoltre un evento `heartbeat` ogni 20 secondi. Non garantisce la
consegna né sostituisce il database: serve a rendere reattiva l'interfaccia. Un
client che si riconnette recupera sempre lo stato corrente dalla control room.

## Errori

Tutti gli errori applicativi usano `application/problem+json`:

```json
{
  "type": "https://example.local/problems/scorta-insufficiente",
  "title": "Scorta insufficiente",
  "status": 409,
  "detail": "Disponibili 3 pezzi, richiesti 5",
  "instance": "/api/v1/prodotti/1/movimenti",
  "code": "SCORTA_INSUFFICIENTE",
  "timestamp": "2026-09-29T14:30:00"
}
```

| Caso | Status | Codice indicativo |
| --- | ---: | --- |
| input non valido | 400 | `VALIDAZIONE_FALLITA` |
| prodotto/categoria inesistente | 404 | `RISORSA_NON_TROVATA` |
| SKU duplicato | 409 | `SKU_GIA_ESISTENTE` |
| scorta insufficiente | 409 | `SCORTA_INSUFFICIENTE` |
| versione concorrente | 409 | `VERSIONE_NON_AGGIORNATA` |
| errore inatteso | 500 | `ERRORE_INTERNO` |
