package com.donquijote.biblioteca;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LibraryService {
    private final BookRepository books;
    private final ReaderRepository readers;
    private final LoanRepository loans;
    private final FineRecordRepository fines;

    public LibraryService(BookRepository books, ReaderRepository readers,
            LoanRepository loans, FineRecordRepository fines) {
        this.books = books;
        this.readers = readers;
        this.loans = loans;
        this.fines = fines;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public List<Map<String, Object>> books() {
        return books.findAll().stream().map(book -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", book.id);
            item.put("title", book.title);
            item.put("author", book.author);
            item.put("code", book.code);
            item.put("publicationYear", book.publicationYear);
            item.put("available", !loans.existsByBookIdAndReturnDateIsNull(book.id));
            return item;
        }).toList();
    }

    public List<Map<String, Object>> readers() {
        return readers.findAll().stream().map(reader -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", reader.id);
            item.put("name", reader.name);
            item.put("dni", reader.dni);
            item.put("address", reader.address);
            item.put("phone", reader.phone);
            long completed = loans.countByReaderIdAndReturnDateIsNotNull(reader.id);
            item.put("readCount", completed);
            item.put("rewardEligible", completed >= 100);
            return item;
        }).toList();
    }

    public List<Map<String, Object>> loans() { return loans(null); }

    public List<Map<String, Object>> loans(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isEmpty() && !List.of("ACTIVO", "VENCIDO", "DEVUELTO").contains(normalized)) {
            throw bad("Estado inválido. Usa ACTIVO, VENCIDO o DEVUELTO");
        }
        return loans.findAll().stream()
                .sorted(Comparator.comparing((Loan loan) -> loan.id).reversed())
                .map(this::view)
                .filter(item -> normalized.isEmpty() || normalized.equals(item.get("status")))
                .toList();
    }

    private Map<String, Object> view(Loan loan) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", loan.id);
        item.put("book", loan.book.title);
        item.put("code", loan.book.code);
        item.put("reader", loan.reader.name);
        item.put("dni", loan.reader.dni);
        item.put("loanDate", loan.loanDate);
        item.put("dueDate", loan.dueDate);
        item.put("returnDate", loan.returnDate);
        LocalDate end = loan.returnDate == null ? LocalDate.now() : loan.returnDate;
        long late = Math.max(0, ChronoUnit.DAYS.between(loan.dueDate, end));
        item.put("fine", late);
        item.put("status", loan.returnDate != null ? "DEVUELTO" : late > 0 ? "VENCIDO" : "ACTIVO");
        return item;
    }

    public List<Map<String, Object>> fines() {
        return fines.findAll().stream()
                .sorted(Comparator.comparing((FineRecord fine) -> fine.id).reversed())
                .map(fine -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", fine.id);
                    item.put("loanId", fine.loan.id);
                    item.put("reader", fine.loan.reader.name);
                    item.put("dni", fine.loan.reader.dni);
                    item.put("book", fine.loan.book.title);
                    item.put("code", fine.loan.book.code);
                    item.put("dueDate", fine.loan.dueDate);
                    item.put("returnDate", fine.loan.returnDate);
                    item.put("recordedAt", fine.recordedAt);
                    item.put("daysLate", fine.daysLate);
                    item.put("amountSoles", fine.amountSoles);
                    return item;
                }).toList();
    }

    @Transactional
    public Book addBook(Book book) {
        book.id = null;
        book.code = book.code.trim().toUpperCase(Locale.ROOT);
        book.title = book.title.trim();
        book.author = book.author.trim();
        if (books.existsByCode(book.code)) throw bad("El código ya está registrado");
        return books.save(book);
    }

    /** Permite editar libros sin perder su historial de préstamos. */
    @Transactional
    public Book editBook(Long id, Book requested) {
        Book stored = books.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
        String code = requested.code.trim().toUpperCase(Locale.ROOT);
        String title = requested.title.trim();
        String author = requested.author.trim();
        if (code.isEmpty() || title.isEmpty() || author.isEmpty()) throw bad("Completa todos los campos del libro");
        if (books.findByCode(code).filter(other -> !other.id.equals(id)).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ya pertenece a otro libro");
        stored.title = title;
        stored.author = author;
        stored.code = code;
        stored.publicationYear = requested.publicationYear;
        return books.save(stored);
    }

    /** No eliminamos registros referenciados por préstamos ni multas. */
    @Transactional
    public void deleteBook(Long id) {
        Book stored = books.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
        if (loans.existsByBookId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede eliminar: este libro tiene préstamos en su historial");
        books.delete(stored);
    }

    @Transactional
    public void deleteReader(Long id) {
        Reader stored = readers.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Lector no encontrado"));
        if (loans.existsByReaderId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede eliminar: este lector tiene préstamos en su historial");
        readers.delete(stored);
    }

    @Transactional
    public Reader addReader(Reader reader) {
        reader.id = null;
        if (readers.existsByDni(reader.dni)) throw bad("El DNI ya está registrado");
        return readers.save(reader);
    }

    @Transactional
    public Map<String, Object> borrow(Long bookId, Long readerId) {
        if (bookId == null || readerId == null) throw bad("Selecciona un libro y un lector");
        if (LocalDate.now().getDayOfWeek() == DayOfWeek.TUESDAY)
            throw bad("Los martes no se permiten préstamos");
        Book book = books.findById(bookId).orElseThrow(() -> bad("Libro inexistente"));
        Reader reader = readers.findById(readerId).orElseThrow(() -> bad("Lector inexistente"));
        if (loans.existsByBookIdAndReturnDateIsNull(bookId)) throw bad("El libro ya está prestado");
        Loan loan = new Loan();
        loan.book = book;
        loan.reader = reader;
        loan.loanDate = LocalDate.now();
        loan.dueDate = loan.loanDate.plusDays(7);
        return view(loans.save(loan));
    }

    @Transactional
    public Map<String, Object> giveBack(Long id) {
        Loan loan = loans.findById(id).orElseThrow(() -> bad("Préstamo inexistente"));
        if (loan.returnDate != null) throw bad("Este préstamo ya fue devuelto");
        loan.returnDate = LocalDate.now();
        Loan saved = loans.save(loan);
        long days = Math.max(0, ChronoUnit.DAYS.between(saved.dueDate, saved.returnDate));
        if (days > 0 && !fines.existsByLoanId(saved.id)) {
            FineRecord record = new FineRecord();
            record.loan = saved;
            record.daysLate = days;
            record.amountSoles = days; // Tarifa acordada: S/ 1 por día.
            record.recordedAt = saved.returnDate;
            fines.save(record);
        }
        return view(saved);
    }

    public Map<String, Object> stats() {
        List<Map<String, Object>> all = loans();
        return Map.of(
            "books", books.count(),
            "readers", readers.count(),
            "active", all.stream().filter(item -> !"DEVUELTO".equals(item.get("status"))).count(),
            "overdue", all.stream().filter(item -> "VENCIDO".equals(item.get("status"))).count()
        );
    }
}
