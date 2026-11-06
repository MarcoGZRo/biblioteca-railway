package com.donquijote.biblioteca;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:crud_test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.demo-data.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class BookReaderCrudIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired ReaderRepository readers;
    @Autowired LoanRepository loans;

    private Book book(String code) {
        Book book = new Book();
        book.code = code; book.title = "Prueba"; book.author = "Autor"; book.publicationYear = 2000;
        return books.save(book);
    }
    private Reader reader(String dni) {
        Reader reader = new Reader();
        reader.name = "Lector prueba"; reader.dni = dni; reader.address = "Lima"; reader.phone = "987654321";
        return readers.save(reader);
    }
    private void historicalLoan(Book book, Reader reader) {
        Loan loan = new Loan();
        loan.book = book; loan.reader = reader;
        loan.loanDate = LocalDate.now().minusDays(10);
        loan.dueDate = loan.loanDate.plusDays(7);
        loan.returnDate = LocalDate.now().minusDays(1);
        loans.save(loan);
    }

    @Test void canEditBookAndDeleteBooksWithoutHistory() throws Exception {
        Book book = book("CRUD-1");
        mvc.perform(put("/api/books/{id}", book.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CRUD-EDITED\",\"title\":\"Título corregido\",\"author\":\"Nuevo autor\",\"publicationYear\":2020}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título corregido"))
                .andExpect(jsonPath("$.code").value("CRUD-EDITED"));
        mvc.perform(delete("/api/books/{id}", book.id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/books/{id}", book.id)).andExpect(status().isNotFound());
    }

    @Test void rejectsDuplicateCodeAndInvalidYear() throws Exception {
        Book book1 = book("CODE-1");
        book("CODE-2");
        mvc.perform(put("/api/books/{id}", book1.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CODE-2\",\"title\":\"Título\",\"author\":\"Autor\",\"publicationYear\":2000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("código")));
        mvc.perform(put("/api/books/{id}", book1.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CODE-3\",\"title\":\"Título\",\"author\":\"Autor\",\"publicationYear\":1200}"))
                .andExpect(status().isBadRequest());
    }

    @Test void preventsDeletingBookOrReaderWithAnyLoanHistory() throws Exception {
        Book book = book("HISTORY-1");
        Reader reader = reader("19999999");
        historicalLoan(book, reader);
        mvc.perform(delete("/api/books/{id}", book.id)).andExpect(status().isConflict());
        mvc.perform(delete("/api/readers/{id}", reader.id)).andExpect(status().isConflict());
    }

    @Test void deletesReaderWithoutHistory() throws Exception {
        Reader reader = reader("18888888");
        mvc.perform(delete("/api/readers/{id}", reader.id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/readers/{id}", reader.id)).andExpect(status().isNotFound());
    }
}
