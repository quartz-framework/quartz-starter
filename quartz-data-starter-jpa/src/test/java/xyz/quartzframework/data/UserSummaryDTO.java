package xyz.quartzframework.data;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryDTO(
        UUID id,
        String username,
        Instant createdAt
) {}
