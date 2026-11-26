package opole.dev.demo.repository;

import jakarta.persistence.QueryHint;
import opole.dev.demo.entity.Wypozyczenia;
import opole.dev.demo.entity.WypozyczeniaId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.stream.Stream;

public interface WypozyczeniaRepository extends JpaRepository<Wypozyczenia, WypozyczeniaId> {
    @Override
    @EntityGraph(attributePaths = {
            "uzytkownik", "egzemplarz", "egzemplarz.ksiazka",
            "egzemplarz.ksiazka.autorzy", "egzemplarz.ksiazka.autorzy.autor"
    })
    List<Wypozyczenia> findAll();

    @Query("""
            select w from Wypozyczenia w
            LEFT JOIN FETCH w.uzytkownik u
            LEFT JOIN FETCH w.egzemplarz e
            LEFT JOIN FETCH e.ksiazka k
            LEFT JOIN FETCH k.autorzy ka
            LEFT JOIN FETCH ka.autor
            """)
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "1000")
    })
    Stream<WypozyczenieProjection> streamAllReadOnly();
}
