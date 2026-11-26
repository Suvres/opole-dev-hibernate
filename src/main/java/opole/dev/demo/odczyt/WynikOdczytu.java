package opole.dev.demo.odczyt;

import java.util.UUID;

public record WynikOdczytu(
        UUID uruchomienieId,
        WariantOdczytu wariant,
        long liczbaRekordow,
        long sumaKontrolna,
        long czasMs,
        long pamiecPrzedBajty,
        long pamiecPoBajty
) {
}
