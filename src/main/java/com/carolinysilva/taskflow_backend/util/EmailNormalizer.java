package com.carolinysilva.taskflow_backend.util;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
