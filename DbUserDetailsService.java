package com.eventmanager.config;

import com.eventmanager.model.AppUser;
import com.eventmanager.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Tells Spring Security how to load a user (by email) from our users table when someone logs in. */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DbUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
        return User.withUsername(user.email())
                .password(user.passwordHash())
                .roles(user.role())                          // USER, ORGANIZER or ADMIN  ->  ROLE_USER, ...
                .disabled(!user.emailVerified())             // can't log in until the email is verified
                .accountLocked(user.isSuspended())           // can't log in while suspended by an admin
                .build();
    }
}
