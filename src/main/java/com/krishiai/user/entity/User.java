package com.krishiai.user.entity;

import com.krishiai.common.audit.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_status", columnList = "status"),
                @Index(name = "idx_users_role_status", columnList = "role, status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uq_users_phone", columnNames = "phone")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @NotBlank(message = "Password hash must not be blank")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "First name must not be blank")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;



    @Pattern(
            regexp = "^\\+?[1-9]\\d{6,14}$",
            message = "Phone must be a valid international phone number (E.164 format, e.g. +9779801234567)"
    )
    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "profile_image_public_id", length = 255)
    private String profileImagePublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    public static User createFarmer(
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String phone
    ) {
        User user = new User();
        user.email = normaliseEmail(email);
        user.passwordHash = passwordHash;
        user.fullName = firstName.strip();
        user.phone = phone != null && !phone.isBlank() ? phone.strip() : null;
        user.role = UserRole.ROLE_FARMER;
        user.status = UserStatus.ACTIVE;
        user.emailVerified = false;
        user.phoneVerified = false;
        user.failedLoginAttempts = 0;
        return user;
    }

    public static User createExpert(
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String phone
    ) {
        User user = new User();
        user.email = normaliseEmail(email);
        user.passwordHash = passwordHash;
        user.fullName = firstName.strip();
        user.phone = phone != null && !phone.isBlank() ? phone.strip() : null;
        user.role = UserRole.ROLE_EXPERT;
        user.status = UserStatus.PENDING; // Must remain PENDING until approved by admin
        user.emailVerified = false;
        user.phoneVerified = false;
        user.failedLoginAttempts = 0;
        return user;
    }

    public static User createAdmin(
            String email,
            String passwordHash,
            String firstName
    ) {
        User user = new User();
        user.email = normaliseEmail(email);
        user.passwordHash = passwordHash;
        user.fullName = firstName.strip();
        user.role = UserRole.ROLE_ADMIN;
        user.status = UserStatus.ACTIVE;
        user.emailVerified = true;
        user.phoneVerified = false;
        user.failedLoginAttempts = 0;
        return user;
    }

    public void recordSuccessfulLogin(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.failedLoginAttempts = 0;
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    public void lockUntil(LocalDateTime until) {
        if (until != null && until.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("lockUntil must be a future timestamp");
        }
        this.lockedUntil = until;
    }

    public void unlock() {
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
    }

    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        this.passwordHash = newPasswordHash;
    }

    public void verifyEmail() {
        this.emailVerified = true;
        if (this.status == UserStatus.PENDING && this.role != UserRole.ROLE_EXPERT) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public static String normaliseEmail(String raw) {
        if (raw == null) return null;
        return raw.strip().toLowerCase();
    }

    public String getFullName() {
        return fullName + " " ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
