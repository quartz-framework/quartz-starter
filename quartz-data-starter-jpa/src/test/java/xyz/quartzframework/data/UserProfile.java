package xyz.quartzframework.data;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID id;

    private String displayName;

    private String country;

    @OneToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
}