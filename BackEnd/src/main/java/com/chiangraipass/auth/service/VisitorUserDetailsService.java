package com.chiangraipass.auth.service;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class VisitorUserDetailsService implements UserDetailsService {

    private final VisitorAccountService accountService;

    public VisitorUserDetailsService(VisitorAccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        VisitorAccount account = accountService.findByEmail(email)
                .filter(found -> found.passwordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
        return new EmailVisitorPrincipal(account);
    }
}
