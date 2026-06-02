package com.healthcare.identityadapter.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_assertions")
public class IdentityAssertionEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String tokenId;

    protected IdentityAssertionEntity() {
    }

    public IdentityAssertionEntity(String id, String status, String subject, String tenantId, String role, String tokenId) {
        this.id = id;
        this.status = status;
        this.subject = subject;
        this.tenantId = tenantId;
        this.role = role;
        this.tokenId = tokenId;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getSubject() {
        return subject;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRole() {
        return role;
    }

    public String getTokenId() {
        return tokenId;
    }
}
