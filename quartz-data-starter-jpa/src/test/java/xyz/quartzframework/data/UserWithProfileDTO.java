package xyz.quartzframework.data;

import java.util.UUID;

public record UserWithProfileDTO(
        UUID id,
        String username,
        String displayName
) {}
