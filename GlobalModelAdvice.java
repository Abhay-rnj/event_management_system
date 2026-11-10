package com.eventmanager.config;

import com.eventmanager.model.AppUser;
import com.eventmanager.service.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Things every page needs: the logged-in user for the navigation bar, and a friendly page for AppException. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUser currentUser;

    public GlobalModelAdvice(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @ModelAttribute("me")
    public AppUser me(Authentication auth) {
        return currentUser.get(auth);
    }

    @ExceptionHandler(AppException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleAppException(AppException ex, Authentication auth, Model model) {
        model.addAttribute("me", currentUser.get(auth));
        model.addAttribute("problem", ex.getMessage());
        return "problem";
    }
}
