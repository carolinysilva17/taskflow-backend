package com.carolinysilva.taskflow_backend.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenRevocationList {

    private final ConcurrentHashMap<String, Instant> revokedTokenExpiryByJti = new ConcurrentHashMap<>();

    public void revoke(String jti, Instant expiresAt) {
        purgeExpired();
        revokedTokenExpiryByJti.put(jti, expiresAt);
    }

    public boolean isRevoked(String jti) {
        Instant expiresAt = revokedTokenExpiryByJti.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (Instant.now().isAfter(expiresAt)) {
            revokedTokenExpiryByJti.remove(jti);
            return false;
        }
        return true;
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        revokedTokenExpiryByJti.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
