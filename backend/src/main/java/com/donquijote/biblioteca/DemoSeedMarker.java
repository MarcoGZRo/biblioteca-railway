package com.donquijote.biblioteca;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Marca persistente para evitar recrear demostraciones eliminadas o editadas. */
@Entity
public class DemoSeedMarker {
    @Id public String id;
    protected DemoSeedMarker() {}
    public DemoSeedMarker(String id) { this.id = id; }
}
