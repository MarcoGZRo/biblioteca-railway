package com.donquijote.biblioteca;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LoanRepository extends JpaRepository<Loan,Long> {
    boolean existsByBookIdAndReturnDateIsNull(Long id);
    boolean existsByBookId(Long id);
    boolean existsByReaderId(Long id);
    long countByReaderIdAndReturnDateIsNotNull(Long id);
    List<Loan> findByReturnDateIsNull();
    List<Loan> findByReturnDateIsNotNull();
    boolean existsByBookIdAndReaderIdAndReturnDateIsNotNull(Long bookId, Long readerId);
}
