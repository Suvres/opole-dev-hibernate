package opole.dev.demo.repository;

import opole.dev.demo.entity.StatusEgzemplarza;
import opole.dev.demo.entity.StatusWypozyczenia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface WypozyczenieProjection {
    WypozyczenieIdProjection getId();

    EgzemplarzProjection getEgzemplarz();

    UzytkownikProjection getUzytkownik();

    OffsetDateTime getTerminZwrotu();

    OffsetDateTime getDataZwrotu();

    StatusWypozyczenia getStatus();

    BigDecimal getKaraNaliczona();

    String getUwagi();

    interface WypozyczenieIdProjection {
        UUID getId();

        OffsetDateTime getDataWypozyczenia();
    }

    interface EgzemplarzProjection {
        UUID getId();

        KsiazkaProjection getKsiazka();

        String getKodKreskowy();

        String getSygnatura();

        StatusEgzemplarza getStatus();

        String getStanFizyczny();

        LocalDate getDataZakupu();

        OffsetDateTime getUtworzonoAt();

        OffsetDateTime getZaktualizowanoAt();
    }

    interface KsiazkaProjection {
        UUID getId();

        String getIsbn();

        String getTytul();

        String getPodtytul();

        Short getRokWydania();

        String getWydawnictwo();

        String getGatunek();

        String getJezyk();

        Integer getLiczbaStron();

        Map<String, Object> getMetadane();

        OffsetDateTime getUtworzonoAt();

        OffsetDateTime getZaktualizowanoAt();

        Set<KsiazkaAutorProjection> getAutorzy();
    }

    interface KsiazkaAutorProjection {
        KsiazkaAutorIdProjection getId();

        AutorProjection getAutor();

        String getRola();
    }

    interface KsiazkaAutorIdProjection {
        UUID getKsiazkaId();

        UUID getAutorId();
    }

    interface AutorProjection {
        UUID getId();

        String getImie();

        String getNazwisko();

        LocalDate getDataUrodzenia();

        String getKrajPochodzenia();

        String getBiografia();

        OffsetDateTime getUtworzonoAt();

        OffsetDateTime getZaktualizowanoAt();
    }

    interface UzytkownikProjection {
        UUID getId();

        String getNumerKarty();

        String getEmail();

        String getImie();

        String getNazwisko();

        String getTelefon();

        Boolean getAktywny();

        Short getLimitWypozyczen();

        OffsetDateTime getUtworzonoAt();

        OffsetDateTime getZaktualizowanoAt();
    }
}
