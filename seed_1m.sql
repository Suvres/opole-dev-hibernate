-- ============================================================================
-- SKRYPT SEEDUJĄCY (MINIMUM 1 000 000 REKORDÓW W KAŻDEJ GŁÓWNEJ TABELI)
-- UWAGA: Tymczasowo wyłącza triggery biznesowe i audytowe dla maksymalnej szybkości
-- ============================================================================

\timing on
SET session_replication_role = 'replica';

-- Tymczasowe wyłączenie triggerów użytkownika na czas masowego ładowania
ALTER TABLE wypozyczenia DISABLE TRIGGER USER;
ALTER TABLE egzemplarze DISABLE TRIGGER USER;
ALTER TABLE ksiazki DISABLE TRIGGER USER;
ALTER TABLE autorzy DISABLE TRIGGER USER;
ALTER TABLE uzytkownicy DISABLE TRIGGER USER;

BEGIN;

-- 1. AUTORZY: 1 000 000 rekordów
INSERT INTO autorzy (id, imie, nazwisko, data_urodzenia, kraj_pochodzenia, biografia)
SELECT
    gen_random_uuid(),
    (ARRAY['Jan', 'Anna', 'Piotr', 'Maria', 'Krzysztof', 'Katarzyna', 'Tomasz', 'Magdalena', 'Paweł', 'Agnieszka'])[1 + (g % 10)],
    (ARRAY['Kowalski', 'Nowak', 'Wiśniewski', 'Wójcik', 'Kowalczyk', 'Kamiński', 'Lewandowski', 'Zieliński', 'Szymański', 'Woźniak'])[1 + ((g / 10) % 10)] || '_' || g,
    DATE '1920-01-01' + ((g % 30000) || ' days')::INTERVAL,
    (ARRAY['Polska', 'USA', 'Wielka Brytania', 'Francja', 'Niemcy', 'Japonia', 'Hiszpania', 'Włochy'])[1 + (g % 8)],
    'Biografia testowa autora numer ' || g
FROM generate_series(1, 1000000) AS g;

-- 2. KSIĄŻKI: 1 000 000 rekordów
INSERT INTO ksiazki (id, isbn, tytul, podtytul, rok_wydania, wydawnictwo, gatunek, jezyk, liczba_stron, metadane)
SELECT
    gen_random_uuid(),
    '978-' || LPAD(g::TEXT, 10, '0'),
    (ARRAY['Cień', 'Tajemnica', 'Kroniki', 'Powrót', 'Ostatni', 'Zaginiony', 'Sekret', 'Droga', 'Światło', 'Mrok'])[1 + (g % 10)]
        || ' ' ||
    (ARRAY['Wiatru', 'Północy', 'Imperium', 'Czasu', 'Przeznaczenia', 'Gwiazd', 'Labiryntu', 'Ognia', 'Wody', 'Ciszy'])[1 + ((g / 10) % 10)]
        || ' #' || g,
    CASE WHEN g % 3 = 0 THEN 'Wydanie rozszerzone tom ' || (1 + (g % 5)) ELSE NULL END,
    (1950 + (g % 77))::SMALLINT,
    (ARRAY['Wydawnictwo Literackie', 'Znak', 'Rebis', 'MAG', 'Fabryka Słów', 'Czarne', 'Prószyński', 'Albatros'])[1 + (g % 8)],
    (ARRAY['Fantastyka', 'Kryminał', 'Reportaż', 'Historia', 'Sci-Fi', 'Biografia', 'Thriller', 'Klasyka'])[1 + (g % 8)],
    (ARRAY['pl', 'en', 'de', 'fr', 'es'])[1 + (g % 5)],
    100 + (g % 900),
    jsonb_build_object(
        'tagi', jsonb_build_array('bestseller', 'seria_' || (g % 50), 'rok_' || (1950 + (g % 77))),
        'ocena_srednia', ROUND((3.0 + (g % 20) * 0.1)::NUMERIC, 1)
    )
FROM generate_series(1, 1000000) AS g;

-- 3. KSIAZKI_AUTORZY: 1 000 000 rekordów (mapowanie 1:1 po pozycji + dodatkowi współautorzy)
WITH a AS (
    SELECT id, ROW_NUMBER() OVER () AS rn FROM autorzy
),
k AS (
    SELECT id, ROW_NUMBER() OVER () AS rn FROM ksiazki
)
INSERT INTO ksiazki_autorzy (ksiazka_id, autor_id, rola)
SELECT k.id, a.id, 'autor'
FROM k
JOIN a ON k.rn = a.rn;

-- 4. EGZEMPLARZE: 1 000 000 rekordów (po 1 egzemplarzu na każdą z 1 mln książek)
WITH k AS (
    SELECT id, ROW_NUMBER() OVER () AS rn FROM ksiazki
)
INSERT INTO egzemplarze (id, ksiazka_id, kod_kreskowy, sygnatura, status, stan_fizyczny, data_zakupu)
SELECT
    gen_random_uuid(),
    k.id,
    'BC-' || LPAD(k.rn::TEXT, 10, '0'),
    'SYG-' || (1 + (k.rn % 100)) || '/' || k.rn,
    CASE
        WHEN k.rn <= 250000 THEN 'wypozyczona'::status_egzemplarza
        WHEN k.rn % 50 = 0 THEN 'zarezerwowana'::status_egzemplarza
        ELSE 'dostepna'::status_egzemplarza
    END,
    (ARRAY['bardzo dobry', 'dobry', 'dostateczny'])[1 + (k.rn % 3)],
    DATE '2020-01-01' + ((k.rn % 2000) || ' days')::INTERVAL
FROM k;

-- 5. UŻYTKOWNICY: 1 000 000 rekordów
INSERT INTO uzytkownicy (id, numer_karty, email, imie, nazwisko, telefon, aktywny, limit_wypozyczen)
SELECT
    gen_random_uuid(),
    'KARTA-' || LPAD(g::TEXT, 9, '0'),
    'czytelnik' || g || '@biblioteka-test.pl',
    (ARRAY['Adam', 'Ewa', 'Michał', 'Karolina', 'Jakub', 'Natalia', 'Mateusz', 'Aleksandra', 'Łukasz', 'Zofia'])[1 + (g % 10)],
    (ARRAY['Mazur', 'Krawczyk', 'Piotrowski', 'Grabowski', 'Nowakowski', 'Pawłowski', 'Michalski', 'Król', 'Wieczorek', 'Jabłoński'])[1 + ((g / 10) % 10)] || '_' || g,
    '+48' || LPAD((500000000 + (g % 499999999))::TEXT, 9, '0'),
    (g % 25 != 0),
    5
FROM generate_series(1, 1000000) AS g;

-- 6. WYPOŻYCZENIA: 1 000 000 rekordów (250 tys. aktywnych + 750 tys. historycznych zwróconych)
-- Daty rozłożone w latach 2025-2026 (trafiają w utworzone partycje!)
WITH e AS (
    SELECT id, status, ROW_NUMBER() OVER () AS rn FROM egzemplarze
),
u AS (
    SELECT id, ROW_NUMBER() OVER () AS rn FROM uzytkownicy
),
dane AS (
    SELECT
        e.id AS egzemplarz_id,
        e.status AS egz_status,
        u.id AS uzytkownik_id,
        e.rn,
        TIMESTAMP WITH TIME ZONE '2025-01-01 08:00:00+00'
            + ((e.rn % 650) || ' days')::INTERVAL
            + ((e.rn % 43200) || ' seconds')::INTERVAL AS data_wyp
    FROM e
    JOIN u ON e.rn = u.rn
)
INSERT INTO wypozyczenia (id, egzemplarz_id, uzytkownik_id, data_wypozyczenia, termin_zwrotu, data_zwrotu, status, kara_naliczona)
SELECT
    gen_random_uuid(),
    egzemplarz_id,
    uzytkownik_id,
    data_wyp,
    data_wyp + INTERVAL '30 days',
    CASE
        WHEN egz_status = 'wypozyczona' THEN NULL
        ELSE data_wyp + ((5 + (rn % 25)) || ' days')::INTERVAL
    END,
    CASE
        WHEN egz_status = 'wypozyczona' THEN 'aktywne'::status_wypozyczenia
        ELSE 'zwrocone'::status_wypozyczenia
    END,
    CASE WHEN rn % 40 = 0 THEN 12.50 ELSE 0.00 END
FROM dane;

-- 7. LOGI AUDYTOWE: 1 000 000 rekordów (aby tabela audytowa również miała min. 1 mln wpisów)
WITH w AS (
    SELECT id, data_wypozyczenia, status, ROW_NUMBER() OVER () AS rn FROM wypozyczenia
)
INSERT INTO logi_audytowe (tabela, rekord_id, operacja, stare_dane, nowe_dane, wykonano_at)
SELECT
    'wypozyczenia',
    w.id,
    'INSERT',
    NULL,
    jsonb_build_object('status', w.status, 'seed_batch', 1),
    w.data_wypozyczenia
FROM w;

COMMIT;

-- Przywrócenie triggerów i normalnego trybu replikacji
ALTER TABLE wypozyczenia ENABLE TRIGGER USER;
ALTER TABLE egzemplarze ENABLE TRIGGER USER;
ALTER TABLE ksiazki ENABLE TRIGGER USER;
ALTER TABLE autorzy ENABLE TRIGGER USER;
ALTER TABLE uzytkownicy ENABLE TRIGGER USER;
SET session_replication_role = 'origin';

-- Odświeżenie zmaterializowanego widoku i statystyk planera PostgreSQL!
REFRESH MATERIALIZED VIEW mv_statystyki_ksiazek;
ANALYZE autorzy;
ANALYZE ksiazki;
ANALYZE ksiazki_autorzy;
ANALYZE egzemplarze;
ANALYZE uzytkownicy;
ANALYZE wypozyczenia;
ANALYZE logi_audytowe;

-- Szybka weryfikacja liczby rekordów
SELECT 'autorzy' AS tabela, COUNT(*) FROM autorzy
UNION ALL SELECT 'ksiazki', COUNT(*) FROM ksiazki
UNION ALL SELECT 'ksiazki_autorzy', COUNT(*) FROM ksiazki_autorzy
UNION ALL SELECT 'egzemplarze', COUNT(*) FROM egzemplarze
UNION ALL SELECT 'uzytkownicy', COUNT(*) FROM uzytkownicy
UNION ALL SELECT 'wypozyczenia', COUNT(*) FROM wypozyczenia
UNION ALL SELECT 'logi_audytowe', COUNT(*) FROM logi_audytowe;
