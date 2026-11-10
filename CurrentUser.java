package com.eventmanager.config;

import com.eventmanager.model.AppUser;
import com.eventmanager.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Finds the logged-in AppUser for the current request. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    /** The logged-in user, or null for visitors who are not logged in. */
    public AppUser get(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        return users.findByEmail(auth.getName()).orElse(null);
    }

    /** Same, but refuses visitors. A user suspended after logging in is also stopped here on the next action. */
    public AppUser require(Authentication auth) {
        AppUser user = get(auth);
        if (user == null || user.isSuspended()) throw new AccessDeniedException("Please log in");
        return user;
    }
}
