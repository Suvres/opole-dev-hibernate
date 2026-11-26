package opole.dev.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "ksiazki_autorzy", schema = "public", indexes = {@Index(name = "idx_ksiazki_autorzy_autor_id",
        columnList = "autor_id")})
public class KsiazkiAutorzy {
    @EmbeddedId
    private KsiazkiAutorzyId id;

    @MapsId("ksiazkaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ksiazka_id", nullable = false)
    private Ksiazki ksiazka;

    @MapsId("autorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "autor_id", nullable = false)
    private Autorzy autor;

    @ColumnDefault("'autor'")
    @Column(name = "rola", nullable = false, length = 50)
    private String rola;


}