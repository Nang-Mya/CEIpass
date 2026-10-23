package com.chiangraipass.auth.dto;

import java.util.UUID;

public record AccountResponse(UUID accountId, String email, String displayName) {
}
