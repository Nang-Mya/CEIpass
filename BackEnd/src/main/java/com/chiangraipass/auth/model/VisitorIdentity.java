package com.chiangraipass.auth.model;

import java.util.UUID;

public interface VisitorIdentity {

    UUID accountId();

    String email();

    String displayName();

     /** Token that ties a sign-in session to the account's current credentials; may be null. */
    default String securityToken() {
        return null;
    }
}
