package com.ubb.genai.organizations.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "organization", schema = "organizations")
public class OrganizationEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 120)
    String name;

    @Column(nullable = false, length = 2000)
    String description;

    @Column(name = "contact_email", nullable = false, length = 254)
    String contactEmail;

    @Column(nullable = false)
    boolean active;

    @Version
    long version;

    protected OrganizationEntity() {
    }

    public OrganizationEntity(UUID id, String name, String description, String contactEmail) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.contactEmail = contactEmail;
        this.active = true;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean active() {
        return active;
    }
}
