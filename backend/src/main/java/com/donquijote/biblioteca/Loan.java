package com.donquijote.biblioteca;
import jakarta.persistence.*;
import java.time.*;
@Entity public class Loan {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
@ManyToOne(optional=false,fetch=FetchType.EAGER) public Book book;
@ManyToOne(optional=false,fetch=FetchType.EAGER) public Reader reader;
public LocalDate loanDate;
public LocalDate dueDate;
public LocalDate returnDate;
public Loan(){}
}
