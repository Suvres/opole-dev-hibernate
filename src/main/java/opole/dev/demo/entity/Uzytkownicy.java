package opole.dev.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "uzytkownicy", schema = "public", indexes = {@Index(name = "idx_uzytkownicy_nazwisko_trgm",
        columnList = "nazwisko")}, uniqueConstraints = {
        @UniqueConstraint(name = "uzytkownicy_numer_karty_key",
                columnNames = {"numer_karty"}),
        @UniqueConstraint(name = "uzytkownicy_email_key",
                columnNames = {"email"})})
public class Uzytkownicy {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "numer_karty", nullable = false, length = 30)
    private String numerKarty;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "imie", nullable = false, length = 100)
    private String imie;

    @Column(name = "nazwisko", nullable = false, length = 150)
    private String nazwisko;

    @Column(name = "telefon", length = 20)
    private String telefon;

    @ColumnDefault("true")
    @Column(name = "aktywny", nullable = false)
    private Boolean aktywny;

    @ColumnDefault("5")
    @Column(name = "limit_wypozyczen", nullable = false)
    private Short limitWypozyczen;

    @ColumnDefault("now()")
    @Column(name = "utworzono_at", nullable = false)
    private OffsetDateTime utworzonoAt;

    @ColumnDefault("now()")
    @Column(name = "zaktualizowano_at", nullable = false)
    private OffsetDateTime zaktualizowanoAt;


}