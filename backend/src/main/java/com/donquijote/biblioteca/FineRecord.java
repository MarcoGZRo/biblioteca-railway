package com.donquijote.biblioteca;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Multa efectivamente registrada tras una devolución fuera de plazo; no representa un cobro. */
@Entity
public class FineRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_id", unique = true, nullable = false)
    public Loan loan;

    @Column(nullable = false)
    public Long daysLate;

    @Column(nullable = false)
    public Long amountSoles;

    @Column(nullable = false)
    public LocalDate recordedAt;

    public FineRecord() {}
}
