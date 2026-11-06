package com.donquijote.biblioteca;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FineRecordRepository extends JpaRepository<FineRecord, Long> {
    boolean existsByLoanId(Long loanId);
    Optional<FineRecord> findByLoanId(Long loanId);
}
