package opole.dev.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "wypozyczenia", schema = "public", indexes = {
        @Index(name = "idx_wypozyczenia_egzemplarz_id",
                columnList = "egzemplarz_id"),
        @Index(name = "idx_wypozyczenia_aktywne",
                columnList = "uzytkownik_id, termin_zwrotu"),
        @Index(name = "idx_wypozyczenia_uzytkownik_id",
                columnList = "uzytkownik_id")})
public class Wypozyczenia {
    @EmbeddedId
    private WypozyczeniaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "egzemplarz_id", nullable = false)
    private Egzemplarze egzemplarz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "uzytkownik_id", nullable = false)
    private Uzytkownicy uzytkownik;

    @Column(name = "termin_zwrotu", nullable = false)
    private OffsetDateTime terminZwrotu;

    @Column(name = "data_zwrotu")
    private OffsetDateTime dataZwrotu;

    @ColumnDefault("'aktywne'")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "status_wypozyczenia not null")
    private StatusWypozyczenia status;

    @ColumnDefault("0.00")
    @Column(name = "kara_naliczona", nullable = false, precision = 10, scale = 2)
    private BigDecimal karaNaliczona;

    @Column(name = "uwagi", length = Integer.MAX_VALUE)
    private String uwagi;


}