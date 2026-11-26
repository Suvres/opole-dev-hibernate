package opole.dev.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "logi_audytowe", schema = "public", indexes = {@Index(name = "idx_logi_audytowe_czas_brin",
        columnList = "wykonano_at")})
public class LogiAudytowe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tabela", nullable = false, length = 50)
    private String tabela;

    @Column(name = "rekord_id", nullable = false)
    private UUID rekordId;

    @Column(name = "operacja", nullable = false, length = 20)
    private String operacja;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stare_dane")
    private Map<String, Object> stareDane;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nowe_dane")
    private Map<String, Object> noweDane;

    @ColumnDefault("now()")
    @Column(name = "wykonano_at", nullable = false)
    private OffsetDateTime wykonanoAt;


}