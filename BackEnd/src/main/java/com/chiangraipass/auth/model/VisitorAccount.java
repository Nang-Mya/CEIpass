package com.chiangraipass.auth.model;

import java.util.UUID;

public record VisitorAccount(UUID accountId, String email, String displayName, String passwordHash) {
}
