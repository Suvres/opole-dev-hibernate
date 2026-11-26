package opole.dev.demo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WypozyczeniaJdbcRepositoryIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private WypozyczeniaJdbcRepository repository;

    @Test
    void pobieraPelnyGrafIGrupujeAutorowWPostgresql() {
        var id = new UUID(0, UUID.randomUUID().getLeastSignificantBits() & Long.MAX_VALUE);
        var ksiazka = UUID.randomUUID();
        var bezAutorow = UUID.randomUUID();
        var egzemplarz1 = UUID.randomUUID();
        var egzemplarz2 = UUID.randomUUID();
        var uzytkownik = UUID.randomUUID();
        var autor1 = UUID.randomUUID();
        var autor2 = UUID.randomUUID();
        var data = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        var suffix = UUID.randomUUID().toString();

        jdbc.update("insert into public.uzytkownicy (id, numer_karty, email, imie, nazwisko) values (?, ?, ?, ?, ?)",
                uzytkownik, suffix.substring(0, 30), suffix + "@example.test", "Czytelnik", "Testowy");
        jdbc.update("insert into public.ksiazki (id, isbn, tytul, metadane) values (?, ?, ?, cast(? as jsonb))",
                ksiazka, suffix.substring(0, 17), "Ksiazka", "{\"tag\": \"test\"}");
        jdbc.update("insert into public.ksiazki (id, isbn, tytul) values (?, ?, ?)",
                bezAutorow, UUID.randomUUID().toString().substring(0, 17), "Bez autorow");
        jdbc.update("insert into public.autorzy (id, imie, nazwisko) values (?, ?, ?), (?, ?, ?)",
                autor1, "Pierwszy", "Autor", autor2, "Drugi", "Autor");
        jdbc.update("insert into public.ksiazki_autorzy (ksiazka_id, autor_id, rola) values (?, ?, ?), (?, ?, ?)",
                ksiazka, autor1, "autor", ksiazka, autor2, "wspolautor");
        jdbc.update("insert into public.egzemplarze (id, ksiazka_id, kod_kreskowy, sygnatura) values (?, ?, ?, ?), (?, ?, ?, ?)",
                egzemplarz1, ksiazka, suffix + "1", "sygnatura1",
                egzemplarz2, bezAutorow, suffix + "2", "sygnatura2");
        jdbc.update("insert into public.wypozyczenia (id, data_wypozyczenia, termin_zwrotu, egzemplarz_id, uzytkownik_id) values (?, ?, ?, ?, ?)",
                id, data, data.plusDays(14), egzemplarz1, uzytkownik);
        jdbc.update("insert into public.wypozyczenia (id, data_wypozyczenia, termin_zwrotu, egzemplarz_id, uzytkownik_id) values (?, ?, ?, ?, ?)",
                id, data.plusDays(1), data.plusDays(15), egzemplarz2, uzytkownik);

        try (var stream = repository.streamAll()) {
            var wynik = stream.filter(w -> w.id().equals(id)).limit(2).toList();
            assertEquals(2, wynik.size());
            assertEquals(data.toInstant(), wynik.get(0).dataWypozyczenia().toInstant());
            assertEquals(data.plusDays(1).toInstant(), wynik.get(1).dataWypozyczenia().toInstant());
            var pierwsze = wynik.get(0);
            assertEquals(uzytkownik, pierwsze.uzytkownik().id());
            assertEquals("Czytelnik", pierwsze.uzytkownik().imie());
            assertEquals(egzemplarz1, pierwsze.egzemplarz().id());
            assertEquals("Ksiazka", pierwsze.egzemplarz().ksiazka().tytul());
            assertEquals("{\"tag\": \"test\"}", pierwsze.egzemplarz().ksiazka().metadaneJson());
            var autorzy = pierwsze.egzemplarz().ksiazka().autorzy();
            assertEquals(2, autorzy.size());
            assertTrue(autorzy.stream().anyMatch(a -> a.autor().id().equals(autor1)
                    && a.autor().imie().equals("Pierwszy") && a.rola().equals("autor")));
            assertTrue(autorzy.stream().anyMatch(a -> a.autor().id().equals(autor2)
                    && a.autor().imie().equals("Drugi") && a.rola().equals("wspolautor")));
            assertTrue(wynik.get(1).egzemplarz().ksiazka().autorzy().isEmpty());
        }
    }
}
