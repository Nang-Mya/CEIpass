package com.chiangraipass.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

public class VisitorExternalIdentityId implements Serializable {

    private String provider;
    private String providerSubject;

    public VisitorExternalIdentityId() {
    }

    public VisitorExternalIdentityId(String provider, String providerSubject) {
        this.provider = provider;
        this.providerSubject = providerSubject;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VisitorExternalIdentityId identityId)) {
            return false;
        }
        return Objects.equals(provider, identityId.provider)
                && Objects.equals(providerSubject, identityId.providerSubject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, providerSubject);
    }
}
