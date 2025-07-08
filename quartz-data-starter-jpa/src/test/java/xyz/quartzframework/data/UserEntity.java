package xyz.quartzframework.data;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    private String username;

    private boolean enabled;

    private Instant createdAt;

    @OneToOne(mappedBy = "user")
    private UserProfile profile;

    public UserEntity(UUID id, String username, boolean enabled, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }
}
