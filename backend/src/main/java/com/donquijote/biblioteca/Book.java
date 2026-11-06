package com.donquijote.biblioteca;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity public class Book {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
@NotBlank public String title;
@NotBlank public String author;
@NotBlank @Column(unique=true,nullable=false) public String code;
@NotNull @Min(1400) @Max(2100) public Integer publicationYear;
public Book(){}
}
