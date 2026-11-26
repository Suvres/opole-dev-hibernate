package opole.dev.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WypozyczenieJdbc(
        UUID id,
        OffsetDateTime dataWypozyczenia,
        UUID egzemplarzId,
        UUID uzytkownikId,
        OffsetDateTime terminZwrotu,
        OffsetDateTime dataZwrotu,
        String status,
        BigDecimal karaNaliczona,
        String uwagi,
        Egzemplarz egzemplarz,
        Uzytkownik uzytkownik
) {
    public record Egzemplarz(UUID id, String kodKreskowy, String sygnatura, String status,
                             String stanFizyczny, Ksiazka ksiazka) {
    }

    public record Ksiazka(UUID id, String isbn, String tytul, String podtytul,
                          List<KsiazkaAutor> autorzy) {
        public Ksiazka {
            autorzy = List.copyOf(autorzy);
        }
    }

    public record KsiazkaAutor(UUID ksiazkaId, UUID autorId, String rola, Autor autor) {
    }

    public record Autor(UUID id, String imie, String nazwisko) {
    }

    public record Uzytkownik(UUID id, String numerKarty, String email, String imie, String nazwisko) {
    }
}
