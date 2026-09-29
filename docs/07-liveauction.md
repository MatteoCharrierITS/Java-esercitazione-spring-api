# LiveAuction — motore live

## Timeline

Per un'asta con `startsAt = 18:30 Europe/Rome`:

```text
18:27  PROGRAMMATA → STANZA_APERTA
       join consentito, offerte vietate

18:30  STANZA_APERTA → APERTA
       endsAt iniziale = 18:37

18:34  offerta valida
       endsAt = 18:37:20

18:37:20  APERTA → CHIUSA, salvo ulteriori rilanci
```

Le date mostrate all'utente sono italiane, mentre API e database usano UTC.

## Programmazione sicura

Pseudoflusso del comando ADMIN:

```text
BEGIN
  valida ruolo, data/ora e prezzo iniziale
  converti Europe/Rome → Instant UTC
  lock prodotto
  verifica astabile e quantita_disponibile > 0
  decrementa disponibile e incrementa bloccata
  crea asta PROGRAMMATA con startsAt e endsAt = startsAt + 7 minuti
COMMIT
```

Il lock impedisce che due richieste usino contemporaneamente l'ultima copia.

## Timer autorevole

Il browser calcola i countdown usando `serverTime`, `startsAt` ed `endsAt`.
Riceve nuovi riferimenti alle transizioni e dopo ogni offerta. Il timer visuale
può arrivare a zero, ma solo il Producer cambia lo stato dell'asta.

A ogni rialzo accettato:

```text
endsAt = endsAt + 20 secondi
```

Non si usa “venti secondi da adesso”: si estende sempre la scadenza corrente.

## Sezione critica di PLACE_BID

```text
BEGIN
  lock asta
  se non APERTA o scaduta: rifiuta
  valida utente, importo e clientBidId
  lock wallet nuovo offerente e precedente leader in ordine di id
  verifica saldo disponibile
  riserva nuovo importo
  libera precedente importo
  salva offerta
  aggiorna leader, prezzo, endsAt +20s e sequence
COMMIT
pubblica BID_ACCEPTED
```

Ordinare i lock dei portafogli per id riduce i deadlock. In caso di conflitto,
la transazione può essere ritentata un numero limitato di volte.

## Settlement

Con vincitore:

1. consuma la riserva del vincitore;
2. accredita il conto amministrativo;
3. decrementa la quantità bloccata del prodotto;
4. assegna una unità all'inventario del vincitore;
5. scrive i movimenti ledger;
6. salva vincitore, prezzo finale e `chiusaAt`;
7. incrementa `sequence` e committa;
8. pubblica `AUCTION_CLOSED` con il vincitore;
9. richiede l'email riepilogativa.

Senza offerte, la quantità passa da bloccata a disponibile. Lock e controllo
dello stato rendono la chiusura idempotente.

## Protocollo eventi

Ogni evento pubblico contiene almeno:

```json
{
  "type": "BID_ACCEPTED",
  "auctionId": 42,
  "sequence": 9,
  "serverTime": "2026-10-03T16:34:12Z"
}
```

Se il client possiede `sequence=7` e riceve `9`, passa a
`SINCRONIZZAZIONE…` e recupera lo snapshot REST.

## Sicurezza

- Solo ADMIN sugli endpoint di programmazione e storico globale.
- Ticket WS monouso, legato a utente e asta, TTL 30 secondi.
- Ticket non emesso prima di `startsAt - 3 minuti`.
- Autorizzazione verificata su ogni comando, non solo al connect.
- Offerte accettate soltanto nello stato `APERTA`.
- Rate limit indicativo: 5 comandi offerta al secondo per utente/asta.
- Importo rifiutato se scala o precisione non sono valide.
- Username mascherato negli eventi pubblici.

## Demo

1. L'ADMIN abilita un prodotto con tre unità e programma un'asta scegliendo
   prezzo iniziale e data/ora.
2. Mostra che disponibile diminuisce e bloccata aumenta.
3. Due utenti entrano nei tre minuti di pre-live senza poter offrire.
4. All'orario previsto l'asta parte con sette minuti.
5. I due utenti rilanciano e vedono prezzo, riserve e `+20s` in tempo reale.
6. Un'offerta oltre il saldo disponibile viene rifiutata.
7. Alla chiusura appare il vincitore e il prodotto entra nel suo inventario.
8. La vittoria compare nello storico personale e nello storico globale ADMIN.
9. Viene generata l'email riepilogativa.
