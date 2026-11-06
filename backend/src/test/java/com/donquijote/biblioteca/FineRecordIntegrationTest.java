package com.donquijote.biblioteca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:biblioteca_fine_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.demo-data.enabled=false"
})
class FineRecordIntegrationTest {

    @Autowired private LibraryService service;
    @Autowired private BookRepository bookRepository;
    @Autowired private ReaderRepository readerRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private FineRecordRepository fineRepository;

    @Test
    void registersOneFineOnLateReturnAndPreservesLoanHistory() {
        Book book = new Book();
        book.title = "Libro de pruebas";
        book.author = "Autor de pruebas";
        book.code = "TEST-FINE-001";
        book.publicationYear = 2020;
        book = bookRepository.save(book);

        Reader reader = new Reader();
        reader.name = "Lector de prueba";
        reader.dni = "87654321";
        reader.address = "Dirección de prueba";
        reader.phone = "999111222";
        reader = readerRepository.save(reader);

        Loan loan = new Loan();
        loan.book = book;
        loan.reader = reader;
        loan.loanDate = LocalDate.now().minusDays(10);
        loan.dueDate = LocalDate.now().minusDays(3);
        loan = loanRepository.save(loan);
        final Long loanId = loan.id;

        assertThat(service.loans("VENCIDO")).hasSize(1);
        var returned = service.giveBack(loanId);
        assertThat(returned.get("status")).isEqualTo("DEVUELTO");
        assertThat(returned.get("fine")).isEqualTo(3L);
        assertThat(service.loans("VENCIDO")).isEmpty();
        assertThat(service.loans("DEVUELTO")).hasSize(1);

        FineRecord fine = fineRepository.findByLoanId(loanId).orElseThrow();
        assertThat(fine.daysLate).isEqualTo(3L);
        assertThat(fine.amountSoles).isEqualTo(3L);
        assertThat(service.fines()).hasSize(1);

        assertThrows(ResponseStatusException.class, () -> service.giveBack(loanId));
        assertThat(fineRepository.count()).isEqualTo(1L);
    }
}
