package com.donquijote.biblioteca;

import java.time.temporal.ChronoUnit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Registra multas de devoluciones tardías existentes antes de incorporar este módulo. */
@Component
@Order(100)
public class FineRecordBackfill implements ApplicationRunner {
    private final LoanRepository loans;
    private final FineRecordRepository fines;

    public FineRecordBackfill(LoanRepository loans, FineRecordRepository fines) {
        this.loans = loans;
        this.fines = fines;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Loan loan : loans.findByReturnDateIsNotNull()) {
            if (fines.existsByLoanId(loan.id)) continue;
            long days = Math.max(0, ChronoUnit.DAYS.between(loan.dueDate, loan.returnDate));
            if (days == 0) continue;
            FineRecord fine = new FineRecord();
            fine.loan = loan;
            fine.daysLate = days;
            fine.amountSoles = days;
            fine.recordedAt = loan.returnDate;
            fines.save(fine);
        }
    }
}
