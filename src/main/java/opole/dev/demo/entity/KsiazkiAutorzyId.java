package opole.dev.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class KsiazkiAutorzyId implements Serializable {
    @Serial
    private static final long serialVersionUID = 6968093541083122239L;
    @Column(name = "ksiazka_id", nullable = false)
    private UUID ksiazkaId;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;


}