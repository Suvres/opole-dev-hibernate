package opole.dev.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ksiazki", schema = "public", indexes = {
        @Index(name = "idx_ksiazki_tytul_trgm",
                columnList = "tytul"),
        @Index(name = "idx_ksiazki_metadane_gin",
                columnList = "metadane")}, uniqueConstraints = {@UniqueConstraint(name = "ksiazki_isbn_key",
        columnNames = {"isbn"})})
public class Ksiazki {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "isbn", nullable = false, length = 17)
    private String isbn;

    @Column(name = "tytul", nullable = false)
    private String tytul;

    @Column(name = "podtytul")
    private String podtytul;

    @Column(name = "rok_wydania")
    private Short rokWydania;

    @Column(name = "wydawnictwo", length = 150)
    private String wydawnictwo;

    @Column(name = "gatunek", length = 100)
    private String gatunek;

    @ColumnDefault("'pl'")
    @Column(name = "jezyk", nullable = false, length = 10)
    private String jezyk;

    @Column(name = "liczba_stron")
    private Integer liczbaStron;

    @ColumnDefault("'{}'")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadane")
    private Map<String, Object> metadane;

    @ColumnDefault("now()")
    @Column(name = "utworzono_at", nullable = false)
    private OffsetDateTime utworzonoAt;

    @ColumnDefault("now()")
    @Column(name = "zaktualizowano_at", nullable = false)
    private OffsetDateTime zaktualizowanoAt;

    @OneToMany(mappedBy = "ksiazka", fetch = FetchType.LAZY)
    private Set<KsiazkiAutorzy> autorzy;
}