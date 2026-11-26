-- Rozszerzenia (UUIDv4/v7 oraz wyszukiwanie pełnotekstowe / trigramy)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Typy wyliczeniowe (ENUM) dla czystości i wydajności danych
CREATE TYPE status_egzemplarza AS ENUM ('dostepna', 'wypozyczona', 'zarezerwowana', 'uszkodzona', 'wycofana');
CREATE TYPE status_wypozyczenia AS ENUM ('aktywne', 'zwrocone', 'przeterminowane', 'zagubione');

-- ============================================================================
-- 1. AUTORZY
-- ============================================================================
CREATE TABLE autorzy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    imie VARCHAR(100) NOT NULL,
    nazwisko VARCHAR(150) NOT NULL,
    data_urodzenia DATE,
    kraj_pochodzenia VARCHAR(100),
    biografia TEXT,
    utworzono_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    zaktualizowano_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 2. KSIĄŻKI (Tytuły / Wydania)
-- ============================================================================
CREATE TABLE ksiazki (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    isbn VARCHAR(17) UNIQUE NOT NULL,
    tytul VARCHAR(255) NOT NULL,
    podtytul VARCHAR(255),
    rok_wydania SMALLINT CHECK (rok_wydania > 0 AND rok_wydania <= EXTRACT(YEAR FROM CURRENT_DATE) + 1),
    wydawnictwo VARCHAR(150),
    gatunek VARCHAR(100),
    jezyk VARCHAR(10) NOT NULL DEFAULT 'pl',
    liczba_stron INTEGER CHECK (liczba_stron > 0),
    metadane JSONB DEFAULT '{}'::jsonb, -- Coś ekstra: elastyczne dane (tagi, nagrody, tłumacz)
    utworzono_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    zaktualizowano_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Relacja wiele-do-wielu (książka może mieć wielu autorów)
CREATE TABLE ksiazki_autorzy (
    ksiazka_id UUID NOT NULL REFERENCES ksiazki(id) ON DELETE CASCADE,
    autor_id UUID NOT NULL REFERENCES autorzy(id) ON DELETE CASCADE,
    rola VARCHAR(50) NOT NULL DEFAULT 'autor', -- np. autor, współautor, redaktor
    PRIMARY KEY (ksiazka_id, autor_id)
);

-- ============================================================================
-- 3. EGZEMPLARZE KSIĄŻEK (Fizyczne kopie na półkach - kluczowe w prawdziwej bibliotece)
-- ============================================================================
CREATE TABLE egzemplarze (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ksiazka_id UUID NOT NULL REFERENCES ksiazki(id) ON DELETE RESTRICT,
    kod_kreskowy VARCHAR(50) UNIQUE NOT NULL,
    sygnatura VARCHAR(50) NOT NULL,
    status status_egzemplarza NOT NULL DEFAULT 'dostepna',
    stan_fizyczny VARCHAR(100) DEFAULT 'dobry',
    data_zakupu DATE DEFAULT CURRENT_DATE,
    utworzono_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    zaktualizowano_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 4. UŻYTKOWNICY (Czytelnicy)
-- ============================================================================
CREATE TABLE uzytkownicy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numer_karty VARCHAR(30) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    imie VARCHAR(100) NOT NULL,
    nazwisko VARCHAR(150) NOT NULL,
    telefon VARCHAR(20),
    aktywny BOOLEAN NOT NULL DEFAULT TRUE,
    limit_wypozyczen SMALLINT NOT NULL DEFAULT 5 CHECK (limit_wypozyczen >= 0),
    utworzono_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    zaktualizowano_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 5. WYPOŻYCZENIA (Partycjonowane po roku dla dużych wolumenów!)
-- ============================================================================
CREATE TABLE wypozyczenia (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    egzemplarz_id UUID NOT NULL REFERENCES egzemplarze(id) ON DELETE RESTRICT,
    uzytkownik_id UUID NOT NULL REFERENCES uzytkownicy(id) ON DELETE RESTRICT,
    data_wypozyczenia TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    termin_zwrotu TIMESTAMPTZ NOT NULL,
    data_zwrotu TIMESTAMPTZ,
    status status_wypozyczenia NOT NULL DEFAULT 'aktywne',
    kara_naliczona NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (kara_naliczona >= 0),
    uwagi TEXT,
    PRIMARY KEY (id, data_wypozyczenia),
    CONSTRAINT chk_daty_wypozyczenia CHECK (termin_zwrotu >= data_wypozyczenia)
) PARTITION BY RANGE (data_wypozyczenia);

-- Partycje na bieżące i przyszłe lata + domyślna
CREATE TABLE wypozyczenia_2025 PARTITION OF wypozyczenia
    FOR VALUES FROM ('2025-01-01 00:00:00+00') TO ('2026-01-01 00:00:00+00');

CREATE TABLE wypozyczenia_2026 PARTITION OF wypozyczenia
    FOR VALUES FROM ('2026-01-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');

CREATE TABLE wypozyczenia_2027 PARTITION OF wypozyczenia
    FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2028-01-01 00:00:00+00');

CREATE TABLE wypozyczenia_default PARTITION OF wypozyczenia DEFAULT;

-- ============================================================================
-- COŚ EKSTRA #1: TABELA AUDYTOWA (Historia zmian statusów wypożyczeń)
-- ============================================================================
CREATE TABLE logi_audytowe (
    id BIGSERIAL PRIMARY KEY,
    tabela VARCHAR(50) NOT NULL,
    rekord_id UUID NOT NULL,
    operacja VARCHAR(20) NOT NULL,
    stare_dane JSONB,
    nowe_dane JSONB,
    wykonano_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEKSY POD DUŻE WOLUMENY
-- ============================================================================

-- 1. Indeksy GIN (trigramy) do błyskawicznego wyszukiwania LIKE / ILIKE '%fraza%' przy milionach rekordów
CREATE INDEX idx_ksiazki_tytul_trgm ON ksiazki USING GIN (tytul gin_trgm_ops);
CREATE INDEX idx_autorzy_nazwisko_trgm ON autorzy USING GIN (nazwisko gin_trgm_ops);
CREATE INDEX idx_uzytkownicy_nazwisko_trgm ON uzytkownicy USING GIN (nazwisko gin_trgm_ops);

-- 2. Indeks GIN na JSONB (wyszukiwanie po tagach/metadanych)
CREATE INDEX idx_ksiazki_metadane_gin ON ksiazki USING GIN (metadane jsonb_path_ops);

-- 3. Indeksy na klucze obce (PostgreSQL nie tworzy ich automatycznie!)
CREATE INDEX idx_ksiazki_autorzy_autor_id ON ksiazki_autorzy(autor_id);
CREATE INDEX idx_egzemplarze_ksiazka_id ON egzemplarze(ksiazka_id);
CREATE INDEX idx_wypozyczenia_egzemplarz_id ON wypozyczenia(egzemplarz_id);
CREATE INDEX idx_wypozyczenia_uzytkownik_id ON wypozyczenia(uzytkownik_id);

-- 4. Indeksy częściowe (Partial Indexes) - oszczędzają miejsce i przyspieszają najczęstsze zapytania
CREATE INDEX idx_egzemplarze_dostepne ON egzemplarze(ksiazka_id) WHERE status = 'dostepna';
CREATE INDEX idx_wypozyczenia_aktywne ON wypozyczenia(uzytkownik_id, termin_zwrotu) WHERE data_zwrotu IS NULL;

-- 5. Indeks BRIN (Block Range Index) - minimalny rozmiar na dysku dla danych rosnących chronologicznie
CREATE INDEX idx_logi_audytowe_czas_brin ON logi_audytowe USING BRIN (wykonano_at);

-- ============================================================================
-- COŚ EKSTRA #2: TRIGGERY AUTOMATYZUJĄCE LOGIKĘ BIZNESOWĄ
-- ============================================================================

-- Automatyczna aktualizacja kolumny zaktualizowano_at
CREATE OR REPLACE FUNCTION aktualizuj_znacznik_czasu()
RETURNS TRIGGER AS $$
BEGIN
    NEW.zaktualizowano_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_autorzy_updated BEFORE UPDATE ON autorzy FOR EACH ROW EXECUTE FUNCTION aktualizuj_znacznik_czasu();
CREATE TRIGGER trg_ksiazki_updated BEFORE UPDATE ON ksiazki FOR EACH ROW EXECUTE FUNCTION aktualizuj_znacznik_czasu();
CREATE TRIGGER trg_egzemplarze_updated BEFORE UPDATE ON egzemplarze FOR EACH ROW EXECUTE FUNCTION aktualizuj_znacznik_czasu();
CREATE TRIGGER trg_uzytkownicy_updated BEFORE UPDATE ON uzytkownicy FOR EACH ROW EXECUTE FUNCTION aktualizuj_znacznik_czasu();

-- Automatyczna zmiana statusu egzemplarza przy wypożyczeniu i zwrocie + walidacja dostępności
CREATE OR REPLACE FUNCTION obsluga_statusu_egzemplarza()
RETURNS TRIGGER AS $$
DECLARE
    aktualny_status status_egzemplarza;
    aktywne_count INTEGER;
    max_limit SMALLINT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- Blokada wiersza egzemplarza (zapobiega race condition przy równoległych wypożyczeniach)
        SELECT status INTO aktualny_status FROM egzemplarze WHERE id = NEW.egzemplarz_id FOR UPDATE;
        
        IF aktualny_status != 'dostepna' THEN
            RAISE EXCEPTION 'Egzemplarz o ID % nie jest dostępny (aktualny status: %)', NEW.egzemplarz_id, aktualny_status;
        END IF;

        -- Sprawdzenie limitu wypożyczeń użytkownika
        SELECT limit_wypozyczen INTO max_limit FROM uzytkownicy WHERE id = NEW.uzytkownik_id;
        SELECT COUNT(*) INTO aktywne_count FROM wypozyczenia WHERE uzytkownik_id = NEW.uzytkownik_id AND data_zwrotu IS NULL;
        
        IF aktywne_count >= max_limit THEN
            RAISE EXCEPTION 'Użytkownik osiągnął maksymalny limit wypożyczeń (%)', max_limit;
        END IF;

        UPDATE egzemplarze SET status = 'wypozyczona' WHERE id = NEW.egzemplarz_id;

    ELSIF TG_OP = 'UPDATE' THEN
        -- Jeśli ustawiono datę zwrotu, oznacz egzemplarz jako dostępny
        IF OLD.data_zwrotu IS NULL AND NEW.data_zwrotu IS NOT NULL THEN
            UPDATE egzemplarze SET status = 'dostepna' WHERE id = NEW.egzemplarz_id;
            IF NEW.status = 'aktywne' THEN
                NEW.status = 'zwrocone';
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_wypozyczenia_egzemplarz
BEFORE INSERT OR UPDATE ON wypozyczenia
FOR EACH ROW EXECUTE FUNCTION obsluga_statusu_egzemplarza();

-- Automatyczny audyt zmian na tabeli wypozyczenia
CREATE OR REPLACE FUNCTION zapisz_audyt_wypozyczen()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO logi_audytowe (tabela, rekord_id, operacja, stare_dane, nowe_dane)
    VALUES (
        'wypozyczenia',
        COALESCE(NEW.id, OLD.id),
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) ELSE NULL END
    );
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audyt_wypozyczenia
AFTER INSERT OR UPDATE OR DELETE ON wypozyczenia
FOR EACH ROW EXECUTE FUNCTION zapisz_audyt_wypozyczen();

-- ============================================================================
-- COŚ EKSTRA #3: WIDOK RAPORTOWY (Zmaterializowany widok top książek)
-- ============================================================================
CREATE MATERIALIZED VIEW mv_statystyki_ksiazek AS
SELECT 
    k.id AS ksiazka_id,
    k.tytul,
    k.isbn,
    COUNT(DISTINCT e.id) AS liczba_egzemplarzy,
    COUNT(w.id) AS laczna_liczba_wypozyczen
FROM ksiazki k
LEFT JOIN egzemplarze e ON k.id = e.ksiazka_id
LEFT JOIN wypozyczenia w ON e.id = w.egzemplarz_id
GROUP BY k.id, k.tytul, k.isbn;

CREATE UNIQUE INDEX idx_mv_statystyki_ksiazek_id ON mv_statystyki_ksiazek(ksiazka_id);
