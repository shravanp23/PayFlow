package com.payflow.merchant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Merchant create(String name, String email) {
        Merchant m = new Merchant();
        m.name = name;
        m.email = email;
        m.status = "ACTIVE";
        m.createdAt = Instant.now();
        return m;
    }
}
