# Interfaccia web e flussi

## Rotte della Consumer

| Rotta | Contenuto |
| --- | --- |
| `/` | redirect a `/dashboard` |
| `/dashboard` | KPI e prodotti da riordinare |
| `/control-room` | Inventory Pulse in tempo reale |
| `/prodotti` | elenco, ricerca, filtri e paginazione |
| `/prodotti/{id}` | dettaglio e ultimi movimenti |
| `/eventi/scorte` | relay SSE Consumer → browser |

La Consumer inoltra alle API solo i parametri noti e conserva i filtri nei link
di paginazione.

## Dashboard

Wireframe indicativo:

```text
┌──────────────────────────────────────────────────────────────┐
│ Catalogo & Magazzino              Dashboard | Prodotti       │
├──────────────────────────────────────────────────────────────┤
│ [20 prodotti] [417 pezzi] [€ 84.572,30] [⚠ 3 sotto scorta] │
│                                                              │
│ Da riordinare                                                │
│ ┌─────────────┬──────────────────────┬──────────┬───────────┐ │
│ │ SKU         │ Prodotto             │ Giacenza │ Soglia    │ │
│ ├─────────────┼──────────────────────┼──────────┼───────────┤ │
│ │ UFF-SCR-001 │ Scrivania regolabile │    2     │    5      │ │
│ └─────────────┴──────────────────────┴──────────┴───────────┘ │
└──────────────────────────────────────────────────────────────┘
```

## Inventory Pulse

È la pagina “da demo”: scura, leggibile anche su uno schermo grande e animata
solo dove comunica un cambiamento reale.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ ● LIVE   INVENTORY PULSE                              14:30:01       │
├─────────────────┬──────────────────────┬─────────────────────────────┤
│ SALUTE          │ VALORE MAGAZZINO     │ ALERT                       │
│    ╭────╮       │ € 84.572,30          │ 2 critici · 3 da riordinare │
│   │ 74% │       │ 417 pezzi            │                             │
│    ╰────╯       │                      │                             │
├─────────────────┴──────────────────────┼─────────────────────────────┤
│ RADAR CATEGORIE                        │ LIVE FEED                   │
│ Informatica  ████████░░ 80             │ 14:30  ↓ 2 Laptop Pro 15   │
│ Accessori    █████████░ 90             │ 14:28  ↑ 8 Hub USB-C       │
│ Ufficio      ████░░░░░░ 40             │ 14:21  = 0 Stampante       │
├────────────────────────────────────────┴─────────────────────────────┤
│ PRIORITÀ RIORDINO                                                   │
│ [CRITICA] Scrivania regolabile · 0/5 · suggeriti 10                 │
│ [ALTA]    Stampante laser       · 2/6 · suggeriti 10                │
└──────────────────────────────────────────────────────────────────────┘
```

Comportamento visivo:

- il pallino `LIVE` è verde quando lo stream SSE è connesso e grigio con testo
  `RICONNESSIONE…` quando cade;
- un movimento nuovo entra in cima al feed con una breve evidenziazione;
- il valore della salute anima la transizione, ma rispetta
  `prefers-reduced-motion`;
- nessun lampeggio continuo e nessun colore senza etichetta testuale;
- sotto i 768 px i pannelli diventano una singola colonna.

È sufficiente HTML, CSS e JavaScript nativo con `EventSource`; un framework
front-end o una libreria di grafici non sono necessari.

Lo stato scorte usa testo e colore, non solo colore:

- `Disponibile` — verde;
- `Sotto soglia` — ambra;
- `Esaurito` — rosso.

## Elenco prodotti

```text
┌──────────────────────────────────────────────────────────────┐
│ Cerca [ laptop / SKU      ] Categoria [Tutte ▼]              │
│ Scorta [Tutte ▼] Ordina [Prezzo decrescente ▼] [Applica]    │
├──────┬─────────────┬──────────────┬──────────┬──────┬────────┤
│ SKU  │ Nome        │ Categoria    │ Prezzo   │ Q.tà │ Stato  │
├──────┼─────────────┼──────────────┼──────────┼──────┼────────┤
│ ...  │ ...         │ ...          │ ...      │ ...  │ ...    │
└──────┴─────────────┴──────────────┴──────────┴──────┴────────┘
                         ‹ Precedente  Pagina 1 di 3  Successiva ›
```

- Il nome porta al dettaglio.
- Con zero risultati si mostra un empty state, non una tabella vuota.
- Parametri errati vengono corretti al default o producono un messaggio chiaro.

## Dettaglio prodotto

Mostra anagrafica, prezzo, categoria, giacenza, soglia e badge dello stato.
Sotto l'anagrafica mostra gli ultimi dieci movimenti dal più recente.

L'inserimento di carichi/scarichi dall'interfaccia è facoltativo: l'endpoint REST
è obbligatorio, mentre la pagina può limitarsi alla consultazione.

## Stati di errore

| Situazione | Comportamento UI |
| --- | --- |
| Producer non raggiungibile | pagina dedicata, messaggio e pulsante “Riprova” |
| prodotto inesistente | pagina 404 coerente con il layout |
| filtri senza risultati | suggerimento di rimuovere uno o più filtri |
| errore inatteso | messaggio generico; dettaglio tecnico solo nei log |

## Accessibilità e formattazione

- Etichette associate ai campi del form.
- Navigazione da tastiera e focus visibile.
- Prezzi formattati in locale italiano (`€ 1.299,90`).
- Date visualizzate in formato italiano, pur mantenendo ISO 8601 nelle API.
- Tabelle con intestazioni semantiche e badge accompagnati da testo.
