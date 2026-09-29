-- Dati dimostrativi per il progetto didattico.
-- Password di tutti gli account demo: Demo123! (hash BCrypt, da cambiare prima di un uso reale).
INSERT INTO utenti (username, email, password_hash, ruolo) VALUES
    ('admin_demo', 'admin@liveauction.example', '$2b$12$kBSvKYdhkNErJYfBZtFjlO9kEe60gMTLj5PHK2EXl2UR3ELVfnVkG', 'ADMIN'),
    ('alice_demo', 'alice@liveauction.example', '$2b$12$kBSvKYdhkNErJYfBZtFjlO9kEe60gMTLj5PHK2EXl2UR3ELVfnVkG', 'USER'),
    ('bruno_demo', 'bruno@liveauction.example', '$2b$12$kBSvKYdhkNErJYfBZtFjlO9kEe60gMTLj5PHK2EXl2UR3ELVfnVkG', 'USER'),
    ('carla_demo', 'carla@liveauction.example', '$2b$12$kBSvKYdhkNErJYfBZtFjlO9kEe60gMTLj5PHK2EXl2UR3ELVfnVkG', 'USER');

INSERT INTO portafogli (utente_id, saldo_totale)
SELECT id, CASE WHEN ruolo = 'ADMIN' THEN 0.00 ELSE 1000.00 END
FROM utenti
WHERE username IN ('admin_demo', 'alice_demo', 'bruno_demo', 'carla_demo');

INSERT INTO movimenti_portafoglio (
    portafoglio_id, tipo, importo, saldo_totale_dopo, saldo_riservato_dopo
)
SELECT p.id, 'IMPOSTAZIONE_SALDO', 1000.00, 1000.00, 0.00
FROM portafogli p
JOIN utenti u ON u.id = p.utente_id
WHERE u.username IN ('alice_demo', 'bruno_demo', 'carla_demo');

INSERT INTO categorie (nome, slug) VALUES
    ('Elettronica', 'elettronica'),
    ('Casa', 'casa'),
    ('Sport', 'sport'),
    ('Libri', 'libri');

INSERT INTO prodotti (
    categoria_id, sku, nome, descrizione, prezzo_fisso, astabile,
    quantita_disponibile, quantita_bloccata
)
SELECT c.id, v.sku, v.nome, v.descrizione, v.prezzo_fisso,
       v.astabile, v.quantita_disponibile, v.quantita_bloccata
FROM (VALUES
    ('elettronica', 'ELE-001', 'Cuffie wireless', 'Cuffie Bluetooth', 79.90, TRUE, 4, 1),
    ('elettronica', 'ELE-002', 'Tastiera meccanica', 'Tastiera compatta', 109.00, TRUE, 1, 1),
    ('elettronica', 'ELE-003', 'Mouse ergonomico', 'Mouse senza fili', 45.00, TRUE, 5, 0),
    ('elettronica', 'ELE-004', 'Webcam HD', 'Webcam USB', 59.00, FALSE, 3, 0),
    ('casa', 'CAS-001', 'Lampada da scrivania', 'Lampada LED regolabile', 39.90, TRUE, 2, 0),
    ('casa', 'CAS-002', 'Set tazze', 'Quattro tazze in ceramica', 24.90, FALSE, 8, 0),
    ('sport', 'SPO-001', 'Tappetino yoga', 'Tappetino antiscivolo', 29.90, TRUE, 4, 0),
    ('sport', 'SPO-002', 'Borraccia termica', 'Borraccia da 750 ml', 19.90, FALSE, 6, 0),
    ('libri', 'LIB-001', 'Manuale Java', 'Introduzione a Java', 34.90, TRUE, 3, 0),
    ('libri', 'LIB-002', 'Quaderno tecnico', 'Quaderno per appunti', 12.90, FALSE, 10, 0)
) AS v(categoria_slug, sku, nome, descrizione, prezzo_fisso, astabile,
       quantita_disponibile, quantita_bloccata)
JOIN categorie c ON c.slug = v.categoria_slug;

-- Le due unità già bloccate in prodotti appartengono a queste aste.
INSERT INTO aste (prodotto_id, admin_id, prezzo_iniziale, inizio_at, fine_at)
SELECT p.id, u.id, 30.00,
       CURRENT_TIMESTAMP + INTERVAL '1 day',
       CURRENT_TIMESTAMP + INTERVAL '1 day 7 minutes'
FROM prodotti p CROSS JOIN utenti u
WHERE p.sku = 'ELE-001' AND u.username = 'admin_demo';

INSERT INTO aste (prodotto_id, admin_id, prezzo_iniziale, inizio_at, fine_at)
SELECT p.id, u.id, 50.00,
       CURRENT_TIMESTAMP + INTERVAL '2 days',
       CURRENT_TIMESTAMP + INTERVAL '2 days 7 minutes'
FROM prodotti p CROSS JOIN utenti u
WHERE p.sku = 'ELE-002' AND u.username = 'admin_demo';
