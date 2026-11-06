package com.donquijote.biblioteca;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final LibraryService service;
    public ApiController(LibraryService service) { this.service = service; }

    @GetMapping("/books") public Object books() { return service.books(); }
    @PostMapping("/books") public Object addBook(@Valid @RequestBody Book book) { return service.addBook(book); }
    @PutMapping("/books/{id}") public Object editBook(@PathVariable Long id, @Valid @RequestBody Book book) {
        return service.editBook(id, book);
    }
    @DeleteMapping("/books/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id) { service.deleteBook(id); }
    @GetMapping("/readers") public Object readers() { return service.readers(); }
    @PostMapping("/readers") public Object addReader(@Valid @RequestBody Reader reader) { return service.addReader(reader); }

    @DeleteMapping("/readers/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteReader(@PathVariable Long id) { service.deleteReader(id); }

    /** Estados admitidos: ACTIVO, VENCIDO, DEVUELTO. */
    @GetMapping("/loans") public Object loans(@RequestParam(required = false) String status) {
        return service.loans(status);
    }
    record BorrowRequest(Long bookId, Long readerId) {}
    @PostMapping("/loans") public Object borrow(@RequestBody BorrowRequest request) {
        return service.borrow(request.bookId(), request.readerId());
    }
    @PostMapping("/loans/{id}/return") public Object giveBack(@PathVariable Long id) {
        return service.giveBack(id);
    }
    @GetMapping("/fines") public Object fines() { return service.fines(); }
    @GetMapping("/stats") public Object stats() { return service.stats(); }
}
