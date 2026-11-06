package com.donquijote.biblioteca;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity public class Reader {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
@NotBlank public String name;
@NotBlank @Pattern(regexp="[0-9]{8}",message="El DNI debe tener 8 dígitos") @Column(unique=true) public String dni;
@NotBlank public String address;
@NotBlank public String phone;
public Reader(){}
}
