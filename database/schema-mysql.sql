-- ================================================================
-- Biblioteca Don Quijote de la Mancha
-- Esquema MySQL 8.0+ / Azure Database for MySQL - Flexible Server
-- Basado en las 5 entidades JPA reales del proyecto adjunto.
-- Seguro para esquemas NUEVOS. No borra datos existentes.
-- ================================================================

CREATE DATABASE IF NOT EXISTS biblioteca_don_quijote
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE biblioteca_don_quijote;

-- 1. Libros
CREATE TABLE IF NOT EXISTS book (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    publication_year INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_book_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Lectores
CREATE TABLE IF NOT EXISTS reader (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    dni VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reader_dni UNIQUE (dni)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Préstamos (el historial no se elimina al devolver)
CREATE TABLE IF NOT EXISTS loan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    loan_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    PRIMARY KEY (id),
    KEY idx_loan_book (book_id),
    KEY idx_loan_reader (reader_id),
    CONSTRAINT fk_loan_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE RESTRICT,
    CONSTRAINT fk_loan_reader FOREIGN KEY (reader_id) REFERENCES reader(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Multas (a lo sumo una multa registrada por préstamo)
CREATE TABLE IF NOT EXISTS fine_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    days_late BIGINT NOT NULL,
    amount_soles BIGINT NOT NULL,
    recorded_at DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_fine_loan UNIQUE (loan_id),
    CONSTRAINT fk_fine_loan FOREIGN KEY (loan_id) REFERENCES loan(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Control de carga de datos de demostración
CREATE TABLE IF NOT EXISTS demo_seed_marker (
    id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verificación opcional:
-- SHOW TABLES;
-- SELECT COUNT(*) FROM book;
-- SELECT COUNT(*) FROM reader;
-- SELECT COUNT(*) FROM loan;
-- SELECT COUNT(*) FROM fine_record;
-- SELECT COUNT(*) FROM demo_seed_marker;
