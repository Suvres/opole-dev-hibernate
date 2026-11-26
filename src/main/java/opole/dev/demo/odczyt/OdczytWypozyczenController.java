package opole.dev.demo.odczyt;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wypozyczenia/odczyt")
public class OdczytWypozyczenController {
    private final OdczytWypozyczenService service;

    public OdczytWypozyczenController(OdczytWypozyczenService service) {
        this.service = service;
    }

    @PostMapping("/{wariant}")
    public WynikOdczytu odczytaj(@PathVariable WariantOdczytu wariant) {
        return service.odczytaj(wariant);
    }
}
