package opole.dev.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Repository
public class WypozyczeniaJdbcRepository {
    private static final int FETCH_SIZE = 1000;
    private static final String SQL = """
            select w.id, w.data_wypozyczenia, w.egzemplarz_id, w.uzytkownik_id,
                   w.termin_zwrotu, w.data_zwrotu, w.status, w.kara_naliczona, w.uwagi,
                   u.id as u_id, u.numer_karty as u_numer_karty, u.email as u_email,
                   u.imie as u_imie, u.nazwisko as u_nazwisko,  e.id as e_id, e.kod_kreskowy as e_kod_kreskowy, e.sygnatura as e_sygnatura,
                   e.status as e_status, e.stan_fizyczny as e_stan_fizyczny, e.ksiazka_id,
                   k.id as k_id, k.isbn as k_isbn, k.tytul as k_tytul, k.podtytul as k_podtytul,
                   ka.ksiazka_id as ka_ksiazka_id, ka.autor_id as ka_autor_id, ka.rola as ka_rola,
                   a.id as a_id, a.imie as a_imie, a.nazwisko as a_nazwisko
            from public.wypozyczenia w
            left join public.uzytkownicy u on w.uzytkownik_id = u.id
            left join public.egzemplarze e on w.egzemplarz_id = e.id
            left join public.ksiazki k on e.ksiazka_id = k.id
            left join public.ksiazki_autorzy ka on ka.ksiazka_id = k.id
            left join public.autorzy a on ka.autor_id = a.id
            order by w.id, w.data_wypozyczenia, ka.autor_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public WypozyczeniaJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Stream<WypozyczenieJdbc> streamAll() {
        Stream<Wiersz> rows = jdbcTemplate.queryForStream(connection -> {
            var statement = connection.prepareStatement(
                    SQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(FETCH_SIZE);
            return statement;
        }, (rs, rowNum) -> mapujWiersz(rs));
        var grouped = new GrupujaceWypozyczenia(rows.iterator());
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(grouped, 0), false)
                .onClose(rows::close);
    }

    private static Wiersz mapujWiersz(ResultSet rs) throws SQLException {
        var uzytkownikId = rs.getObject("u_id", UUID.class);
        var uzytkownik = uzytkownikId == null ? null : new WypozyczenieJdbc.Uzytkownik(
                uzytkownikId, rs.getString("u_numer_karty"), rs.getString("u_email"),
                rs.getString("u_imie"), rs.getString("u_nazwisko"));
        var ksiazkaId = rs.getObject("k_id", UUID.class);
        var ksiazka = ksiazkaId == null ? null : new WypozyczenieJdbc.Ksiazka(
                ksiazkaId, rs.getString("k_isbn"), rs.getString("k_tytul"), rs.getString("k_podtytul"), List.of());
        var egzemplarzId = rs.getObject("e_id", UUID.class);
        var egzemplarz = egzemplarzId == null ? null : new WypozyczenieJdbc.Egzemplarz(
                egzemplarzId, rs.getString("e_kod_kreskowy"), rs.getString("e_sygnatura"),
                rs.getString("e_status"), rs.getString("e_stan_fizyczny"),  ksiazka);
        var autorId = rs.getObject("a_id", UUID.class);
        var autor = autorId == null ? null : new WypozyczenieJdbc.Autor(
                autorId, rs.getString("a_imie"), rs.getString("a_nazwisko"));
        var powiazanieId = rs.getObject("ka_autor_id", UUID.class);
        var powiazanie = powiazanieId == null ? null : new WypozyczenieJdbc.KsiazkaAutor(
                rs.getObject("ka_ksiazka_id", UUID.class), powiazanieId, rs.getString("ka_rola"), autor);
        var wypozyczenie = new WypozyczenieJdbc(
                rs.getObject("id", UUID.class), rs.getObject("data_wypozyczenia", OffsetDateTime.class),
                rs.getObject("egzemplarz_id", UUID.class), rs.getObject("uzytkownik_id", UUID.class),
                rs.getObject("termin_zwrotu", OffsetDateTime.class), rs.getObject("data_zwrotu", OffsetDateTime.class),
                rs.getString("status"), rs.getBigDecimal("kara_naliczona"), rs.getString("uwagi"),
                egzemplarz, uzytkownik);
        return new Wiersz(wypozyczenie, powiazanie);
    }

    private record Wiersz(WypozyczenieJdbc wypozyczenie, WypozyczenieJdbc.KsiazkaAutor autor) {
    }

    private static final class GrupujaceWypozyczenia implements Iterator<WypozyczenieJdbc> {
        private final Iterator<Wiersz> rows;
        private Wiersz nastepny;

        private GrupujaceWypozyczenia(Iterator<Wiersz> rows) {
            this.rows = rows;
        }

        @Override
        public boolean hasNext() {
            return nastepny != null || rows.hasNext();
        }

        @Override
        public WypozyczenieJdbc next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var pierwszy = nastepny != null ? nastepny : rows.next();
            nastepny = null;
            var wypozyczenie = pierwszy.wypozyczenie();
            var autorzy = new ArrayList<WypozyczenieJdbc.KsiazkaAutor>();
            if (pierwszy.autor() != null) {
                autorzy.add(pierwszy.autor());
            }
            while (rows.hasNext()) {
                var row = rows.next();
                if (!wypozyczenie.id().equals(row.wypozyczenie().id())
                        || !wypozyczenie.dataWypozyczenia().toInstant()
                        .equals(row.wypozyczenie().dataWypozyczenia().toInstant())) {
                    nastepny = row;
                    break;
                }
                if (row.autor() != null) {
                    autorzy.add(row.autor());
                }
            }
            var e = wypozyczenie.egzemplarz();
            if (e == null || e.ksiazka() == null) {
                return wypozyczenie;
            }
            var k = e.ksiazka();
            var ksiazka = new WypozyczenieJdbc.Ksiazka(k.id(), k.isbn(), k.tytul(), k.podtytul(), autorzy);
            var egzemplarz = new WypozyczenieJdbc.Egzemplarz(e.id(), e.kodKreskowy(), e.sygnatura(),
                    e.status(), e.stanFizyczny(), ksiazka);
            return new WypozyczenieJdbc(wypozyczenie.id(), wypozyczenie.dataWypozyczenia(),
                    wypozyczenie.egzemplarzId(), wypozyczenie.uzytkownikId(), wypozyczenie.terminZwrotu(),
                    wypozyczenie.dataZwrotu(), wypozyczenie.status(), wypozyczenie.karaNaliczona(),
                    wypozyczenie.uwagi(), egzemplarz, wypozyczenie.uzytkownik());
        }
    }
}
