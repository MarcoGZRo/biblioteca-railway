package com.donquijote.biblioteca;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ReaderRepository extends JpaRepository<Reader,Long> {
    boolean existsByDni(String dni);
    Optional<Reader> findByDni(String dni);
}
