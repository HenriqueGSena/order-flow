package com.backend.orderflow.domain;

import com.backend.orderflow.domain.enuns.ApplicationLoggerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationLogger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "operation", length = 100, nullable = false)
    private String operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", length = 20, nullable = false)
    private ApplicationLoggerType logType;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "json_envio", columnDefinition = "JSON")
    private String jsonEnvio;

    @Column(name = "json_retorno", columnDefinition = "JSON")
    private String jsonRetorno;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
