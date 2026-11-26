package opole.dev.demo.repository;

import opole.dev.demo.entity.Wypozyczenia;
import opole.dev.demo.entity.WypozyczeniaId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WypozyczeniaLazyRepository extends JpaRepository<Wypozyczenia, WypozyczeniaId> {
}
