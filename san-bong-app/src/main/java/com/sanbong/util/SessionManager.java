package com.sanbong.util;

import com.sanbong.model.User;

/** Holds the currently logged-in user for the duration of the app session. */
public final class SessionManager {

    private static User currentUser;

    private SessionManager() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isAdmin() {
        return currentUser != null && "admin".equals(currentUser.getRole());
    }

    public static boolean isVenueOwner() {
        return currentUser != null && "venue_owner".equals(currentUser.getRole());
    }

    /** Scope to pass into owner-aware DAO/service queries: null means "no restriction" (admin sees everything). */
    public static Integer ownerScope() {
        return isAdmin() ? null : currentUser.getId();
    }

    public static void logout() {
        currentUser = null;
    }
}
