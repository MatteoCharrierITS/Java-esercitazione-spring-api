# Contratti REST e WebSocket

## Convenzioni

- Base REST: `http://localhost:8081/api/v1`.
- Date persistite e restituite in UTC ISO 8601.
- L'input amministrativo `LocalDateTime` è interpretato in `Europe/Rome`.
- Crediti con `BigDecimal` e due decimali.
- Errori REST in `application/problem+json`.
- Operazioni protette con bearer token gestito dalla Consumer.
- Gli endpoint `/admin/**` richiedono il ruolo `ADMIN`.

## REST

| Metodo | Endpoint | Accesso | Scopo |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | pubblico | registra utente e wallet |
| `POST` | `/auth/login` | pubblico | apre una sessione e restituisce i token |
| `POST` | `/auth/refresh` | pubblico | rinnova i token usando il refresh token |
| `POST` | `/auth/logout` | Bearer | revoca la sessione corrente |
| `DELETE` | `/me` | Bearer | disattiva e anonimizza il proprio account |
| `GET` | `/prodotti` | pubblico | catalogo paginato |
| `GET` | `/prodotti/{id}` | pubblico | dettaglio, stock e `astabile` |
| `POST` | `/prodotti/{id}/acquisti` | USER | acquisto fisso |
| `GET` | `/me/inventario` | USER | prodotti posseduti |
| `GET` | `/me/portafoglio` | USER | saldo e movimenti |
| `PUT` | `/me/portafoglio/impostazioni` | USER | imposta saldo finto |
| `GET` | `/me/vittorie` | USER | storico personale vittorie |
| `GET` | `/aste` | pubblico | lobby filtrabile |
| `GET` | `/aste/{id}` | pubblico | snapshot autorevole |
| `POST` | `/aste/{id}/ticket` | USER | ticket WebSocket breve |
| `POST` | `/admin/aste` | ADMIN | programma asta e blocca stock |
| `POST` | `/admin/aste/{id}/annullamento` | ADMIN | annulla quando consentito |
| `GET` | `/admin/aste/storico` | ADMIN | storico globale e vincitori |
| `PUT` | `/admin/prodotti/{id}` | ADMIN | modifica prodotto, stock e flag |

## Autenticazione

### Regole di accesso nel Producer

Il Producer valida il JWT e usa il suo `sub` come ID utente. A ogni richiesta
protetta controlla che la sessione sia valida e che l'utente sia ancora attivo;
legge poi il ruolo corrente dal database. `Ruolo.USER` e `Ruolo.ADMIN` diventano
rispettivamente le autorità Spring `ROLE_USER` e `ROLE_ADMIN`. Perciò
`hasRole('ADMIN')` verifica `ROLE_ADMIN`, senza affidarsi a un ruolo nel JWT.

Le regole HTTP di `SecurityConfig` rendono pubblici solo registrazione, login,
refresh, health e i `GET` sotto `/prodotti/**` e `/aste/**`. `/admin/**`
richiede `ADMIN`, `/me/**` richiede `USER`; `DELETE /me` richiede un utente
autenticato. Gli altri percorsi richiedono almeno l'autenticazione. Una nuova
operazione riservata sotto un percorso pubblico, anche se è un `GET`, deve avere
una regola HTTP più specifica, posta prima della regola pubblica.

`@EnableMethodSecurity` abilita `@PreAuthorize` sui metodi dei bean Spring.
I nuovi servizi devono dichiarare esplicitamente le regole di ruolo o di
proprietà della risorsa che servono: la sola abilitazione non assegna permessi
ai metodi. Per esempio `hasRole('ADMIN')` richiede il ruolo ADMIN; nel servizio
di eliminazione account il `sub` del token deve corrispondere all'ID passato al
metodo. Qui `authentication` è un `JwtAuthenticationToken`, `principal` è il
JWT, `authentication.name` è lo username e `authentication.token.subject` è
l'ID utente.

`POST /api/v1/auth/register` accetta `username`, `email` e `password`, crea un
utente con ruolo `USER` e un portafoglio iniziale a zero. Restituisce `201` con
`id`, `username`, `email` e `ruolo`; username o email già in uso restituiscono
`409`.

`POST /api/v1/auth/login` accetta `username` e `password`. Restituisce un
`accessToken` JWT, `tokenType: "Bearer"`, `expiresAt`, `refreshToken`,
`refreshExpiresAt`, `userId`, `username` e `ruolo`. Credenziali errate o un
account disattivato restituiscono `401`. La Consumer conserva entrambi i token
nella propria sessione server-side e invia l'access token al Producer con
`Authorization: Bearer <accessToken>`. L'access token dura 30 minuti e il
refresh token 7 giorni. Il Producer verifica a ogni richiesta che la sessione
sia valida e che l'utente esista, sia attivo e abbia il ruolo necessario.

`POST /api/v1/auth/refresh` accetta `{ "refreshToken": "..." }` senza header
Bearer e restituisce lo stesso formato del login. Ogni rinnovo sostituisce il
refresh token precedente; quello vecchio restituisce `401`. La scadenza della
sessione resta quella fissata al login. `POST /api/v1/auth/logout` richiede
`Authorization: Bearer <accessToken>` e restituisce `204` senza body. Revoca la
sessione corrente: tutti gli access token e il refresh token di quella sessione
diventano inutilizzabili. La Consumer deve eliminare entrambi i token dalla
propria sessione dopo il logout.

`DELETE /api/v1/me` richiede `Authorization: Bearer <accessToken>` e
restituisce `204` senza body. Disattiva l'account (`attivo = false`), sostituisce
username, email e hash della password con valori anonimi e revoca tutte le
sessioni dell'utente. Gli ID e le relazioni storiche restano nel database.
Login, refresh e access token già emessi non funzionano più. Dopo la risposta,
la Consumer elimina i token dalla propria sessione. Username ed email originali
possono essere registrati nuovamente.

## Catalogo

```http
GET /api/v1/prodotti?query=laptop&categoria=informatica&astabile=true&page=0&size=12
```

```json
{
  "content": [{
    "id": 1,
    "sku": "INF-LAP-001",
    "nome": "Laptop Pro 15",
    "prezzoFisso": 1299.90,
    "astabile": true,
    "quantitaDisponibile": 3,
    "quantitaBloccata": 1,
    "asteProgrammate": 1
  }],
  "page": 0,
  "size": 12,
  "totalElements": 1
}
```

`astabile` è un campo persistito, non è derivato da altri valori. La possibilità
effettiva di programmare una nuova asta richiede anche
`quantitaDisponibile > 0`.

## Portafoglio

```http
PUT /api/v1/me/portafoglio/impostazioni
```

```json
{ "saldoTotale": 10000.00 }
```

Risposta:

```json
{
  "saldoTotale": 10000.00,
  "saldoRiservato": 1250.00,
  "saldoDisponibile": 8750.00,
  "valuta": "CRD"
}
```

Un valore inferiore al riservato produce
`409 SALDO_INFERIORE_AL_RISERVATO`.

## Programmazione di un'asta

```http
POST /api/v1/admin/aste
```

```json
{
  "prodottoId": 1,
  "inizioLocale": "2026-10-03T18:30:00",
  "timeZone": "Europe/Rome",
  "prezzoIniziale": 500.00
}
```

Il server:

1. verifica il ruolo ADMIN;
2. converte la data locale in UTC;
3. blocca la riga prodotto;
4. verifica `astabile = true` e `quantitaDisponibile > 0`;
5. sposta una unità da disponibile a bloccata;
6. crea l'asta con stato `PROGRAMMATA`, apertura stanza a `startsAt - 3m` e
   `endsAt = startsAt + 7m`.

L'ADMIN creatore è identificato dal `sub` del token autenticato, non dal body.
Il servizio richiede ruolo ADMIN e verifica che l'ID creatore corrisponda al
`sub` e a un utente attivo con ruolo ADMIN nel database.

Risposta `201` con `Location: /api/v1/aste/42`.

Il body usa il DTO di creazione, con una sintesi del prodotto e orari UTC:

```json
{
  "id": 42,
  "stato": "PROGRAMMATA",
  "prodotto": { "id": 1, "nome": "Laptop Pro 15" },
  "prezzoIniziale": 500.00,
  "incrementoMinimo": 1.00,
  "aperturaStanzaAt": "2026-10-03T16:27:00Z",
  "inizioAt": "2026-10-03T16:30:00Z",
  "fineAt": "2026-10-03T16:37:00Z",
  "serverTime": "2026-10-02T10:30:00Z",
  "sequence": 0
}
```

L'apertura della stanza è calcolata a meno tre minuti; la transizione automatica
di stato viene gestita dal motore temporale. La risposta non contiene entity
JPA, credenziali o dati dell'ADMIN.

Errori rilevanti:

- `422 PRODOTTO_NON_ASTABILE`;
- `409 PRODOTTO_NON_DISPONIBILE`;
- `422 DATA_INIZIO_NON_VALIDA`;
- `422 PREZZO_INIZIALE_NON_VALIDO`.

Gli stessi codici `422` si applicano a data, fuso e prezzo mancanti o non
validi nel body. JSON malformato o `prodottoId` mancante/non positivo producono
`400`; un prodotto inesistente produce `404 RISORSA_NON_TROVATA`.
Gli errori del body e di dominio sono restituiti come `application/problem+json`.

## Lobby delle aste

`GET /api/v1/aste` è pubblico e implementato. Accetta:

- `stato`: uno dei valori `PROGRAMMATA`, `STANZA_APERTA`, `APERTA`, `CHIUSA`,
  `ANNULLATA`; se omesso, include tutti gli stati.
- `categoria`: slug esatto della categoria del prodotto.
- `query`: testo cercato nel nome, SKU o descrizione del prodotto, senza
  distinzione tra maiuscole e minuscole. Gli spazi esterni vengono rimossi;
  `%`, `_` e gli altri caratteri vengono trattati come testo letterale.
- `page`: indice da zero, predefinito `0`.
- `size`: da `1` a `100`, predefinito `12`.

I filtri si combinano; categoria e testo vuoti equivalgono a filtri assenti.
L'ordinamento è `inizioAt` crescente, poi `id` crescente. Le aste restano
consultabili anche se il prodotto viene disattivato. Una pagina oltre i
risultati o filtri senza corrispondenze restituiscono `200` e `content: []`.

```http
GET /api/v1/aste?stato=APERTA&categoria=informatica&query=laptop&page=0&size=12
```

```json
{
  "content": [{
    "id": 42,
    "stato": "APERTA",
    "prodotto": { "id": 1, "nome": "Laptop Pro 15" },
    "prezzoIniziale": 500.00,
    "incrementoMinimo": 1.00,
    "offertaCorrente": 630.00,
    "numeroOfferte": 2,
    "aperturaStanzaAt": "2026-10-03T16:27:00Z",
    "inizioAt": "2026-10-03T16:30:00Z",
    "fineAt": "2026-10-03T16:37:40Z",
    "offerteConsentite": true,
    "sequence": 4
  }],
  "page": 0,
  "size": 12,
  "totalElements": 1,
  "totalPages": 1,
  "serverTime": "2026-10-03T16:31:00Z"
}
```

Pagina, dimensione, stato o ID non interpretabili producono `400` in
`application/problem+json`. Il servizio applica anche il controllo della
paginazione (`400 PARAMETRI_NON_VALIDI`) quando viene invocato direttamente.
Il totale e i risultati della pagina vengono letti dalla stessa fotografia
transazionale del database.

## Snapshot asta

`GET /api/v1/aste/{id}` è pubblico e implementato. Un'asta inesistente
restituisce `404 RISORSA_NON_TROVATA` in `application/problem+json`.

```json
{
  "id": 42,
  "stato": "STANZA_APERTA",
  "prodotto": { "id": 1, "nome": "Laptop Pro 15" },
  "prezzoIniziale": 500.00,
  "incrementoMinimo": 1.00,
  "offertaCorrente": null,
  "migliorOfferente": null,
  "numeroOfferte": 0,
  "aperturaStanzaAt": "2026-10-03T16:27:00Z",
  "inizioAt": "2026-10-03T16:30:00Z",
  "fineAt": "2026-10-03T16:37:00Z",
  "serverTime": "2026-10-03T16:28:05Z",
  "offerteConsentite": false,
  "sequence": 1
}
```

In `CHIUSA` lo snapshot include anche:

```json
{
  "vincitore": { "id": 9, "displayName": "g***i" },
  "prezzoFinale": 630.00,
  "chiusaAt": "2026-10-03T16:38:40Z"
}
```

`numeroOfferte` conta le offerte persistite; `migliorOfferente` identifica
l'autore dell'offerta più alta, con username mascherato come `g***i`.
Senza offerte, `offertaCorrente` e `migliorOfferente` sono `null` e il conteggio
è zero. `prezzoFinale` deriva da `offertaCorrente` solo in `CHIUSA` con vincitore;
senza vincitore, `vincitore` e `prezzoFinale` vengono omessi. I campi di esito
non sono esposti prima della chiusura; `chiusaAt` è presente se registrato.

Stato, sequence, timer, leader e conteggio provengono da una sola lettura
SQL, anche in presenza di rilanci concorrenti. Non vengono esposte entity JPA,
email, credenziali o dati dell'ADMIN. `offerteConsentite` è `true` solo se lo
stato è `APERTA` e `inizioAt <= serverTime < fineAt`; indica la disponibilità
temporale, mentre l'operazione di offerta verifica anche ruolo e crediti.
Il timer conserva eventuali estensioni già persistite.

Entrambe le letture restituiscono `Cache-Control: no-store`, orari UTC e tempo
server; non attivano aste e non modificano stock. Le transizioni restano
responsabilità dello scheduler. Ticket, annullamento e storici sono ancora
previsti dal contratto e non implementati nel modulo aste di questo branch.

## Storici

`GET /api/v1/me/vittorie` mostra esclusivamente le aste vinte dall'utente
autenticato, con prodotto, importo e data. `GET /api/v1/admin/aste/storico`
restituisce tutte le aste concluse e permette filtri per prodotto, vincitore e
intervallo temporale.

## WebSocket/STOMP

Handshake: `ws://localhost:8081/ws?ticket={ticketMonouso}`.

Il ticket può essere richiesto solo da tre minuti prima dell'inizio. Prima di
quel momento il server risponde `409 STANZA_NON_APERTA`.

| Direzione | Destinazione | Messaggio |
| --- | --- | --- |
| client → server | `/app/aste/{id}/join` | ingresso stanza |
| client → server | `/app/aste/{id}/offerte` | rilancio, solo in `APERTA` |
| server → stanza | `/topic/aste/{id}` | eventi pubblici ordinati |
| server → utente | `/user/queue/aste` | conferme/rifiuti privati |

Comando offerta:

```json
{
  "type": "PLACE_BID",
  "clientBidId": "5ab0d96e-c7b0-42d1-a74b-97180d9849b8",
  "importo": 630.00,
  "knownSequence": 8
}
```

Evento pubblico accettato:

```json
{
  "type": "BID_ACCEPTED",
  "auctionId": 42,
  "sequence": 9,
  "importo": 630.00,
  "offerenteDisplay": "g***i",
  "numeroOfferte": 9,
  "fineAt": "2026-10-03T16:38:40Z",
  "serverTime": "2026-10-03T16:34:12Z",
  "extensionSeconds": 20
}
```

Rifiuto privato per fondi insufficienti:

```json
{
  "type": "BID_REJECTED",
  "clientBidId": "5ab0d96e-c7b0-42d1-a74b-97180d9849b8",
  "code": "SALDO_INSUFFICIENTE",
  "message": "Saldo disponibile insufficiente",
  "snapshotRequired": false
}
```

Eventi pubblici principali: `ROOM_OPENED`, `AUCTION_STARTED`,
`AUCTION_SNAPSHOT`, `USER_JOINED`, `BID_ACCEPTED`, `TIMER_EXTENDED`,
`AUCTION_CLOSED`, `AUCTION_CANCELLED`.

## Errori principali

| Status/canale | Codice |
| --- | --- |
| 401 | `AUTENTICAZIONE_RICHIESTA` |
| 403 | `OPERAZIONE_NON_CONSENTITA` |
| 404 | `RISORSA_NON_TROVATA` |
| 409 | `STANZA_NON_APERTA` |
| 409 | `ASTA_NON_APERTA` |
| 409 | `PRODOTTO_NON_DISPONIBILE` |
| 409 | `OFFERTA_SUPERATA` |
| 409 | `SALDO_INSUFFICIENTE` |
| 422 | `PRODOTTO_NON_ASTABILE` |
| 422 | `DATA_INIZIO_NON_VALIDA` |
| coda privata | `SEQUENCE_NON_AGGIORNATA` |
