# Collection Postman

Importare `local.postman_environment.json` e selezionare l'ambiente
**LiveAuction • Locale**. Importare poi le collection desiderate:

| File | Modulo | Stato |
| --- | --- | --- |
| `auth.json` | Registrazione e login | Implementato |
| `prodotti.json` | Catalogo, acquisti, gestione ADMIN | API previste |
| `inventario.json` | Inventario personale | API prevista |
| `portafoglio.json` | Saldo e impostazioni | API previste |
| `aste.json` | Lobby, ticket, programmazione e storici | API previste |

L'ambiente usa `http://localhost:8081/api/v1` come `baseUrl`. Modificarlo se
il Producer è esposto su un'altra porta. Le credenziali iniziali sono quelle
degli account demo creati da Flyway; usarle soltanto in locale. I token restano
nelle variabili dell'ambiente Postman e non sono salvati nei file del progetto.

Eseguire `auth.json` nell'ordine numerato per verificare registrazione,
validazione, login USER e login ADMIN. I due login demo impostano `userToken` e
`adminToken`, usati automaticamente dalle altre collection. Se un token scade,
rieseguire il rispettivo login. Le richieste di registrazione generano un nome
diverso a ogni esecuzione.

Le altre collection seguono [il contratto REST](../04-api-rest.md). Finché i
relativi controller non saranno sviluppati, una risposta `404` è attesa. I body
segnati come **provvisori** vanno aggiornati quando il modulo definirà i propri
DTO. Impostare `prodottoId` e `astaId` nell'ambiente usando ID esistenti; la
richiesta di programmazione salva `astaId` dalla risposta `Location`, se
presente.

Il protocollo STOMP su `/ws` richiede un client WebSocket: `aste.json` include
la richiesta REST del ticket, ma non i comandi e le sottoscrizioni STOMP.
