package opole.dev.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "egzemplarze", schema = "public", indexes = {
        @Index(name = "idx_egzemplarze_dostepne",
                columnList = "ksiazka_id"),
        @Index(name = "idx_egzemplarze_ksiazka_id",
                columnList = "ksiazka_id")}, uniqueConstraints = {@UniqueConstraint(name = "egzemplarze_kod_kreskowy_key",
        columnNames = {"kod_kreskowy"})})
public class Egzemplarze {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "ksiazka_id", nullable = false)
    private Ksiazki ksiazka;

    @Column(name = "kod_kreskowy", nullable = false, length = 50)
    private String kodKreskowy;

    @Column(name = "sygnatura", nullable = false, length = 50)
    private String sygnatura;

    @ColumnDefault("'dostepna'")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "status_egzemplarza not null")
    private StatusEgzemplarza status;

    @ColumnDefault("'dobry'")
    @Column(name = "stan_fizyczny", length = 100)
    private String stanFizyczny;

    @ColumnDefault("CURRENT_DATE")
    @Column(name = "data_zakupu")
    private LocalDate dataZakupu;

    @ColumnDefault("now()")
    @Column(name = "utworzono_at", nullable = false)
    private OffsetDateTime utworzonoAt;

    @ColumnDefault("now()")
    @Column(name = "zaktualizowano_at", nullable = false)
    private OffsetDateTime zaktualizowanoAt;


}