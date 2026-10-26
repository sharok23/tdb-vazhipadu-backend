package com.tdb.vazhipadu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "temple")
public class Temple {

    @Id
    private Long id;

    protected Temple() {
    }
}
