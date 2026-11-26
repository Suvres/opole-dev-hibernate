package opole.dev.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "autorzy", schema = "public", indexes = {@Index(name = "idx_autorzy_nazwisko_trgm",
        columnList = "nazwisko")})
public class Autorzy {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "imie", nullable = false, length = 100)
    private String imie;

    @Column(name = "nazwisko", nullable = false, length = 150)
    private String nazwisko;

    @Column(name = "data_urodzenia")
    private LocalDate dataUrodzenia;

    @Column(name = "kraj_pochodzenia", length = 100)
    private String krajPochodzenia;

    @Column(name = "biografia", length = Integer.MAX_VALUE)
    private String biografia;

    @ColumnDefault("now()")
    @Column(name = "utworzono_at", nullable = false)
    private OffsetDateTime utworzonoAt;

    @ColumnDefault("now()")
    @Column(name = "zaktualizowano_at", nullable = false)
    private OffsetDateTime zaktualizowanoAt;


}