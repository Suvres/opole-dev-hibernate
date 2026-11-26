package opole.dev.demo.odczyt;

import jakarta.persistence.EntityManager;
import opole.dev.demo.entity.Wypozyczenia;
import opole.dev.demo.repository.WypozyczeniaJdbcRepository;
import opole.dev.demo.repository.WypozyczeniaLazyRepository;
import opole.dev.demo.repository.WypozyczeniaRepository;
import opole.dev.demo.repository.WypozyczenieProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class OdczytWypozyczenService {
    private static final Logger log = LoggerFactory.getLogger(OdczytWypozyczenService.class);
    private static final int BATCH_SIZE = 1000;

    private final WypozyczeniaRepository eagerRepository;
    private final WypozyczeniaLazyRepository lazyRepository;
    private final WypozyczeniaJdbcRepository jdbcRepository;
    private final EntityManager entityManager;

    public OdczytWypozyczenService(WypozyczeniaRepository eagerRepository,
                                   WypozyczeniaLazyRepository lazyRepository,
                                   WypozyczeniaJdbcRepository jdbcRepository,
                                   EntityManager entityManager) {
        this.eagerRepository = eagerRepository;
        this.lazyRepository = lazyRepository;
        this.jdbcRepository = jdbcRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public WynikOdczytu odczytaj(WariantOdczytu wariant) {
        long start = System.nanoTime();
        long pamiecPrzed = zajetaPamiec();
        UUID uruchomienieId = UUID.randomUUID();
        var licznik = new Licznik();
        log.info("Odczyt wypozyczen start uruchomienie_id={} wariant={} pamiec_przed_bajty={}",
                uruchomienieId, wariant, pamiecPrzed);
        try {
            switch (wariant) {
                case EAGER -> eagerRepository.findAll().forEach(w -> przetworz(w, licznik));
                case LAZY -> lazyRepository.findAll().forEach(w -> przetworz(w, licznik));
                case HIBERNATE_STREAM -> przetworzHibernate(licznik, false, uruchomienieId);
                case HIBERNATE_BATCH -> przetworzHibernate(licznik, true, uruchomienieId);
                case JDBC_BATCH -> {
                    try (var stream = jdbcRepository.streamAll()) {
                        stream.forEach(w -> {
                            licznik.dodaj(w.id(), w.dataWypozyczenia());
                            if (licznik.liczba % BATCH_SIZE == 0) {
                                log.info("Odczyt wypozyczen postep uruchomienie_id={} wariant={} rekordy={}",
                                        uruchomienieId, wariant, licznik.liczba);
                            }
                        });
                    }
                }
            }
            var wynik = new WynikOdczytu(uruchomienieId, wariant, licznik.liczba, licznik.suma,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
                    pamiecPrzed, zajetaPamiec());
            log.info("Odczyt wypozyczen koniec uruchomienie_id={} wariant={} rekordy={} suma_kontrolna={} czas_ms={} pamiec_przed_bajty={} pamiec_po_bajty={}",
                    uruchomienieId, wynik.wariant(), wynik.liczbaRekordow(), wynik.sumaKontrolna(),
                    wynik.czasMs(), wynik.pamiecPrzedBajty(), wynik.pamiecPoBajty());
            return wynik;
        } catch (RuntimeException e) {
            log.error("Odczyt wypozyczen blad uruchomienie_id={} wariant={} rekordy={}",
                    uruchomienieId, wariant, licznik.liczba, e);
            throw e;
        }
    }

    private void przetworzHibernate(Licznik licznik, boolean czyscKontekst, UUID uruchomienieId) {
        try (Stream<WypozyczenieProjection> stream = eagerRepository.streamAllReadOnly()) {
            stream.forEach(w -> {
                licznik.dodaj(w.getId().getId(), w.getId().getDataWypozyczenia());
                if (czyscKontekst && licznik.liczba % BATCH_SIZE == 0) {
                    entityManager.clear();
                    log.info("Odczyt wypozyczen postep uruchomienie_id={} wariant={} rekordy={} kontekst_wyczyszczony={}",
                            uruchomienieId, WariantOdczytu.HIBERNATE_BATCH, licznik.liczba, czyscKontekst);

                } else {
                    log.info("Odczyt wypozyczen postep uruchomienie_id={} wariant={} rekordy={} kontekst_wyczyszczony={}",
                            uruchomienieId, WariantOdczytu.HIBERNATE_STREAM, licznik.liczba, czyscKontekst);

                }

            });
        }
        System.gc();
    }

    private static void przetworz(Wypozyczenia wypozyczenie, Licznik licznik) {
        licznik.dodaj(wypozyczenie.getId().getId(), wypozyczenie.getId().getDataWypozyczenia());
    }

    private static long zajetaPamiec() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static class Licznik {
        private long liczba;
        private long suma;

        private void dodaj(UUID id, OffsetDateTime dataWypozyczenia) {
            liczba++;
            var instant = dataWypozyczenia.toInstant();
            suma += id.getMostSignificantBits() ^ id.getLeastSignificantBits()
                    ^ instant.getEpochSecond() ^ instant.getNano();
        }
    }
}
