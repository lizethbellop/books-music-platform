package com.musa.users.session;

import java.time.Instant;

public record RefreshSession(
        String email,
        Instant sessionExpiresAt,
        boolean rememberMe
) {}