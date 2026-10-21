package com.chiangraipass.auth.model;

import java.util.UUID;

public interface VisitorIdentity {

    UUID accountId();

    String email();

    String displayName();
}
