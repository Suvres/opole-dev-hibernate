package opole.dev.demo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WypozyczeniaJdbcRepositoryTest {
    @Test
    void grupujeAutorowWedlugPelnegoKluczaIZamykaStrumien() throws Exception {
        var template = mock(JdbcTemplate.class);
        var connection = mock(Connection.class);
        var statement = mock(PreparedStatement.class);
        var id = UUID.randomUUID();
        var ksiazkaId = UUID.randomUUID();
        var autor1 = UUID.randomUUID();
        var autor2 = UUID.randomUUID();
        var data = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        var row1 = wiersz(id, data, ksiazkaId, autor1);
        var row2 = wiersz(id, data, ksiazkaId, autor2);
        var row3 = wiersz(id, data.plusDays(1), ksiazkaId, null);
        var closed = new AtomicBoolean();
        when(connection.prepareStatement(anyString(), eq(ResultSet.TYPE_FORWARD_ONLY),
                eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(statement);
        when(template.queryForStream(any(PreparedStatementCreator.class),
                org.mockito.ArgumentMatchers.<RowMapper<Object>>any()))
                .thenAnswer(invocation -> {
                    PreparedStatementCreator creator = invocation.getArgument(0);
                    RowMapper<?> mapper = invocation.getArgument(1);
                    assertEquals(statement, creator.createPreparedStatement(connection));
                    return Stream.of(mapper.mapRow(row1, 0), mapper.mapRow(row2, 1),
                            mapper.mapRow(row3, 2)).onClose(() -> closed.set(true));
                });

        try (var stream = new WypozyczeniaJdbcRepository(template).streamAll()) {
            var wynik = stream.toList();
            assertEquals(2, wynik.size());
            assertEquals(data, wynik.get(0).dataWypozyczenia());
            assertEquals(data.plusDays(1), wynik.get(1).dataWypozyczenia());
            assertEquals("Czytelnik", wynik.get(0).uzytkownik().imie());
            assertEquals("Kod", wynik.get(0).egzemplarz().kodKreskowy());
            assertEquals("Tytul", wynik.get(0).egzemplarz().ksiazka().tytul());
            assertEquals("{\"tag\": 1}", wynik.get(0).egzemplarz().ksiazka().metadaneJson());
            var autorzy = wynik.get(0).egzemplarz().ksiazka().autorzy();
            assertEquals(2, autorzy.size());
            assertEquals(autor1, autorzy.get(0).autor().id());
            assertEquals(autor2, autorzy.get(1).autor().id());
            assertEquals("Autor", autorzy.get(0).autor().imie());
            assertEquals("wspolautor", autorzy.get(0).rola());
            assertTrue(wynik.get(1).egzemplarz().ksiazka().autorzy().isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> autorzy.clear());
        }
        assertTrue(closed.get());
        verify(statement).setFetchSize(1000);
    }

    @Test
    void zamykaStrumienPoWczesnymZakonczeniu() throws Exception {
        var template = mock(JdbcTemplate.class);
        var closed = new AtomicBoolean();
        var rs1 = wiersz(UUID.randomUUID(), OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                UUID.randomUUID(), null);
        var rs2 = wiersz(UUID.randomUUID(), OffsetDateTime.parse("2026-01-02T10:00:00Z"),
                UUID.randomUUID(), null);
        when(template.queryForStream(any(PreparedStatementCreator.class),
                org.mockito.ArgumentMatchers.<RowMapper<Object>>any()))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return Stream.of(mapper.mapRow(rs1, 0), mapper.mapRow(rs2, 1))
                            .onClose(() -> closed.set(true));
                });
        try (var stream = new WypozyczeniaJdbcRepository(template).streamAll()) {
            assertTrue(stream.findFirst().isPresent());
        }
        assertTrue(closed.get());
    }

    private static ResultSet wiersz(UUID id, OffsetDateTime data, UUID ksiazkaId, UUID autorId)
            throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getObject("data_wypozyczenia", OffsetDateTime.class)).thenReturn(data);
        when(rs.getObject("u_id", UUID.class)).thenReturn(UUID.randomUUID());
        when(rs.getObject("e_id", UUID.class)).thenReturn(UUID.randomUUID());
        when(rs.getObject("k_id", UUID.class)).thenReturn(ksiazkaId);
        when(rs.getObject("a_id", UUID.class)).thenReturn(autorId);
        when(rs.getObject("ka_autor_id", UUID.class)).thenReturn(autorId);
        when(rs.getObject("ka_ksiazka_id", UUID.class)).thenReturn(ksiazkaId);
        when(rs.getString("u_imie")).thenReturn("Czytelnik");
        when(rs.getString("e_kod_kreskowy")).thenReturn("Kod");
        when(rs.getString("k_tytul")).thenReturn("Tytul");
        when(rs.getString("k_metadane")).thenReturn("{\"tag\": 1}");
        when(rs.getString("a_imie")).thenReturn("Autor");
        when(rs.getString("ka_rola")).thenReturn("wspolautor");
        return rs;
    }
}
