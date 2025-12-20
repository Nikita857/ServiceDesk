package com.bm.wschat.feature.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_telegram_id", columnList = "telegramId"),
        @Index(name = "idx_user_domain_account", columnList = "domainAccount")
})

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_seq"
    )
    @SequenceGenerator(
            name = "user_seq",
            sequenceName = "users_id_seq",
            allocationSize = 1
    )
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(length = 150)
    private String fio;

    @Column(length = 200, unique = true)
    private String email;

    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(name = "domain_account")
    private String domainAccount;

    @Column(nullable = false)
    private boolean specialist;

    @Column(name = "refresh_token_expiry_date")
    private Instant refreshTokenExpiryDate;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"user_id", "role"}
            )
    )
    @Column(name = "role", length = 50, nullable = false)
    private Set<String> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles == null ? Set.of() : roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public boolean isAdmin() {
        return roles != null && roles.contains(SenderType.ADMIN.name());
    }

    public boolean isSpecialist() {
        Set<String> specialistRoles = Set.of(SenderType.SYSADMIN.name(), SenderType.DEV1C.name(), SenderType.DEVELOPER.name());
        return specialist || roles.stream().anyMatch(specialistRoles::contains);
    }

}
