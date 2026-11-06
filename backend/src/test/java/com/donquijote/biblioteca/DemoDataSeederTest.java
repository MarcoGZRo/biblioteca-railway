package com.donquijote.biblioteca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:biblioteca_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.demo-data.enabled=true"
})
class DemoDataSeederTest {

    @Autowired private BookRepository books;
    @Autowired private ReaderRepository readers;
    @Autowired private LoanRepository loans;
    @Autowired private DemoDataSeeder seeder;
    @Autowired private FineRecordRepository fines;
    @Autowired private FineRecordBackfill fineBackfill;
    @Autowired private LibraryService service;

    @Test
    void loadsDemoDataOnlyOnceAndShowsRewardEligibility() {
        assertThat(books.count()).isGreaterThanOrEqualTo(15);
        assertThat(readers.count()).isGreaterThanOrEqualTo(8);

        Reader honored = readers.findByDni("90000001").orElseThrow();
        assertThat(loans.countByReaderIdAndReturnDateIsNotNull(honored.id)).isEqualTo(100);
        assertThat(service.readers().stream()
                .filter(r -> "90000001".equals(r.get("dni")))
                .findFirst().orElseThrow().get("rewardEligible")).isEqualTo(true);

        long previousLoans = loans.count();
        seeder.run(new DefaultApplicationArguments(new String[0]));
        assertThat(loans.count()).isEqualTo(previousLoans);
        assertThat(service.stats().get("overdue")).isEqualTo(4L);
        assertThat(service.loans("VENCIDO")).hasSize(4);
        assertThat(service.loans("ACTIVO")).hasSize(1);
        for (int i = 6; i <= 8; i++) {
            String dni = String.format("900000%02d", i);
            Reader reader = readers.findByDni(dni).orElseThrow();
            assertThat(service.loans("VENCIDO")).anyMatch(loan -> dni.equals(loan.get("dni")));
            assertThat(loans.countByReaderIdAndReturnDateIsNotNull(reader.id)).isZero();
        }
        assertThat(service.loans("DEVUELTO").size()).isGreaterThanOrEqualTo(103);
        assertThat(service.fines()).isNotEmpty();
        long previousFines = fines.count();
        fineBackfill.run(new DefaultApplicationArguments(new String[0]));
        assertThat(fines.count()).isEqualTo(previousFines);
    }
}
