package opole.dev.demo.odczyt;

import jakarta.persistence.EntityManager;
import opole.dev.demo.entity.Wypozyczenia;
import opole.dev.demo.entity.WypozyczeniaId;
import opole.dev.demo.repository.WypozyczenieJdbc;
import opole.dev.demo.repository.WypozyczenieProjection;
import opole.dev.demo.repository.WypozyczeniaJdbcRepository;
import opole.dev.demo.repository.WypozyczeniaLazyRepository;
import opole.dev.demo.repository.WypozyczeniaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OdczytWypozyczenServiceTest {
    private final WypozyczeniaRepository eager = mock(WypozyczeniaRepository.class);
    private final WypozyczeniaLazyRepository lazy = mock(WypozyczeniaLazyRepository.class);
    private final WypozyczeniaJdbcRepository jdbc = mock(WypozyczeniaJdbcRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final OdczytWypozyczenService service = new OdczytWypozyczenService(eager, lazy, jdbc, entityManager);

    @Test
    void wszystkieWariantyPrzetwarzajaTeSameKluczeIPaczkowyCzysciKontekst() {
        var data = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        var wypozyczenia = IntStream.range(0, 2005).mapToObj(i -> {
            var id = new WypozyczeniaId();
            id.setId(new UUID(0, i));
            id.setDataWypozyczenia(data.plusSeconds(i));
            var w = new Wypozyczenia();
            w.setId(id);
            return w;
        }).toList();
        var rekordy = wypozyczenia.stream().map(w -> new WypozyczenieJdbc(
                w.getId().getId(), w.getId().getDataWypozyczenia(), null, null,
                null, null, null, null, null, null, null)).toList();
        var projekcje = wypozyczenia.stream().map(w -> projection(
                w.getId().getId(), w.getId().getDataWypozyczenia())).toList();
        var zamkniete = new ArrayList<AtomicBoolean>();
        when(eager.findAll()).thenReturn(wypozyczenia);
        when(lazy.findAll()).thenReturn(wypozyczenia);
        when(eager.streamAllReadOnly()).thenAnswer(invocation -> {
            var closed = new AtomicBoolean();
            zamkniete.add(closed);
            return projekcje.stream().onClose(() -> closed.set(true));
        });
        when(jdbc.streamAll()).thenAnswer(invocation -> {
            var closed = new AtomicBoolean();
            zamkniete.add(closed);
            return rekordy.stream().onClose(() -> closed.set(true));
        });

        var wyniki = IntStream.range(0, WariantOdczytu.values().length)
                .mapToObj(i -> service.odczytaj(WariantOdczytu.values()[i])).toList();

        assertTrue(wyniki.stream().allMatch(w -> w.liczbaRekordow() == 2005));
        assertTrue(wyniki.stream().allMatch(w -> w.sumaKontrolna() == wyniki.getFirst().sumaKontrolna()));
        assertEquals(3, zamkniete.size());
        assertTrue(zamkniete.stream().allMatch(AtomicBoolean::get));
        verify(entityManager, times(2)).clear();
    }

    @Test
    void zamykaStrumienPoBledzie() {
        var closed = new AtomicBoolean();
        var row = projection(null, null);
        var rows = java.util.stream.Stream.of(row).onClose(() -> closed.set(true));
        when(eager.streamAllReadOnly()).thenReturn(rows);

        assertThrows(NullPointerException.class, () -> service.odczytaj(WariantOdczytu.HIBERNATE_BATCH));
        assertTrue(closed.get());
        verify(entityManager, never()).clear();
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "999, 0", "1000, 1", "1001, 1", "2000, 2"})
    void czysciKontekstTylkoPoPelnychPaczkach(int liczba, int oczekiwanaLiczbaCzyszczen) {
        var id = new WypozyczeniaId();
        id.setId(UUID.randomUUID());
        id.setDataWypozyczenia(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        when(eager.streamAllReadOnly()).thenReturn(IntStream.range(0, liczba)
                .mapToObj(i -> projection(id.getId(), id.getDataWypozyczenia())));

        assertEquals(liczba, service.odczytaj(WariantOdczytu.HIBERNATE_BATCH).liczbaRekordow());
        verify(entityManager, times(oczekiwanaLiczbaCzyszczen)).clear();
    }

    private static WypozyczenieProjection projection(UUID id, OffsetDateTime dataWypozyczenia) {
        var projectedId = mock(WypozyczenieProjection.WypozyczenieIdProjection.class);
        when(projectedId.getId()).thenReturn(id);
        when(projectedId.getDataWypozyczenia()).thenReturn(dataWypozyczenia);
        var projection = mock(WypozyczenieProjection.class);
        when(projection.getId()).thenReturn(projectedId);
        return projection;
    }
}
