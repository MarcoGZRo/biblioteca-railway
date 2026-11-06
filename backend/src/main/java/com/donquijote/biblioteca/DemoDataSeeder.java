package com.donquijote.biblioteca;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

/** Datos ficticios para la demostración académica. Se insertan de forma idempotente. */
@Component
@Order(10)
public class DemoDataSeeder implements ApplicationRunner {
    private final BookRepository books;
    private final ReaderRepository readers;
    private final LoanRepository loans;
    private final DemoSeedMarkerRepository seedMarkers;

    @Value("${app.demo-data.enabled:true}")
    private boolean enabled;

    public DemoDataSeeder(BookRepository books, ReaderRepository readers, LoanRepository loans, DemoSeedMarkerRepository seedMarkers) {
        this.books = books;
        this.readers = readers;
        this.loans = loans;
        this.seedMarkers = seedMarkers;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || seedMarkers.existsById("initial-data-v5")) return;

        String[][] sampleBooks = {
            {"Don Quijote de la Mancha", "Miguel de Cervantes", "1605"},
            {"La casa de los espíritus", "Isabel Allende", "1982"},
            {"Cien años de soledad", "Gabriel García Márquez", "1967"},
            {"El principito", "Antoine de Saint-Exupéry", "1943"},
            {"Los ríos profundos", "José María Arguedas", "1958"},
            {"Tradiciones peruanas", "Ricardo Palma", "1872"},
            {"La ciudad y los perros", "Mario Vargas Llosa", "1963"},
            {"El mundo es ancho y ajeno", "Ciro Alegría", "1941"},
            {"La metamorfosis", "Franz Kafka", "1915"},
            {"El túnel", "Ernesto Sábato", "1948"},
            {"Ficciones", "Jorge Luis Borges", "1944"},
            {"Paco Yunque", "César Vallejo", "1931"},
            {"El caballero Carmelo", "Abraham Valdelomar", "1913"},
            {"Aves sin nido", "Clorinda Matto de Turner", "1889"},
            {"La palabra del mudo", "Julio Ramón Ribeyro", "1973"}
        };
        List<Book> catalog = new ArrayList<>();
        for (int i = 0; i < sampleBooks.length; i++) {
            String code = String.format("DEMO-%03d", i + 1);
            String[] info = sampleBooks[i];
            Book book = books.findByCode(code).orElseGet(() -> {
                Book b = new Book();
                b.code = code;
                b.title = info[0];
                b.author = info[1];
                b.publicationYear = Integer.parseInt(info[2]);
                return books.save(b);
            });
            catalog.add(book);
        }

        String[][] sampleReaders = {
            {"Alonso Quijano (demo: 100 lecturas)", "90000001", "Calle La Mancha 100", "987654321"},
            {"Dulcinea Toboso", "90000002", "Jr. Las Flores 120", "987654322"},
            {"Sancho Panza", "90000003", "Av. Los Olivos 335", "987654323"},
            {"Marcela Rojas", "90000004", "Jr. Libertad 42", "987654324"},
            {"Pedro Salazar", "90000005", "Av. Principal 670", "987654325"},
            {"Elena Valdez (demo: vencido)", "90000006", "Jr. Grau 341", "987654326"},
            {"Mateo Flores (demo: vencido)", "90000007", "Av. Amazonas 164", "987654327"},
            {"Rosa Cárdenas (demo: vencido)", "90000008", "Jr. Comercio 510", "987654328"}
        };
        List<Reader> people = new ArrayList<>();
        for (String[] info : sampleReaders) {
            Reader reader = readers.findByDni(info[1]).orElseGet(() -> {
                Reader r = new Reader();
                r.name = info[0];
                r.dni = info[1];
                r.address = info[2];
                r.phone = info[3];
                return readers.save(r);
            });
            people.add(reader);
        }

        // El primer lector ya alcanzó 100 devoluciones y puede exhibir la recompensa.
        Reader honored = people.get(0);
        long completed = loans.countByReaderIdAndReturnDateIsNotNull(honored.id);
        LocalDate today = LocalDate.now();
        for (long i = completed; i < 100; i++) {
            LocalDate date = today.minusDays(230).plusDays(i);
            saveLoan(catalog.get((int)(i % 8)), honored, date, date.plusDays(5));
        }

        // Otros lectores: lecturas completadas y casos activos/vencidos para pruebas.
        seedOnceReturned(catalog.get(8), people.get(1), today.minusDays(40), today.minusDays(35));
        seedOnceReturned(catalog.get(9), people.get(2), today.minusDays(25), today.minusDays(18));
        // Demostración de una multa ya registrada, tras devolver con retraso.
        if (!loans.existsByBookIdAndReaderIdAndReturnDateIsNotNull(catalog.get(9).id, people.get(3).id)) {
            saveLoan(catalog.get(9), people.get(3), today.minusDays(40), today.minusDays(29));
        }
        seedOnceActive(catalog.get(10), people.get(3), today.minusDays(12)); // vencido
        seedOnceActive(catalog.get(11), people.get(4), today.minusDays(2)); // activo

        // Tres casos NUEVOS, independientes, de préstamos vencidos (sin devolución).
        // Cada uno utiliza un libro y un lector diferentes; no se duplican al reiniciar.
        seedOnceActive(catalog.get(12), people.get(5), today.minusDays(10)); // 3 días de atraso
        seedOnceActive(catalog.get(13), people.get(6), today.minusDays(16)); // 9 días de atraso
        seedOnceActive(catalog.get(14), people.get(7), today.minusDays(22)); // 15 días de atraso

        seedMarkers.save(new DemoSeedMarker("initial-data-v5"));
    }

    private void seedOnceReturned(Book b, Reader r, LocalDate loanDate, LocalDate returnDate) {
        if (!loans.existsByBookId(b.id)) saveLoan(b, r, loanDate, returnDate);
    }

    private void seedOnceActive(Book b, Reader r, LocalDate loanDate) {
        if (!loans.existsByBookId(b.id)) saveLoan(b, r, loanDate, null);
    }

    private void saveLoan(Book book, Reader reader, LocalDate date, LocalDate returned) {
        Loan l = new Loan();
        l.book = book;
        l.reader = reader;
        l.loanDate = date;
        l.dueDate = date.plusDays(7);
        l.returnDate = returned;
        loans.save(l);
    }
}
