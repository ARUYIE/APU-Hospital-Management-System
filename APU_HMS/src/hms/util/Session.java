package hms.util;

import hms.role.User;

/**
 * Holds the currently logged-in user for the lifetime of the running application.
 * Simple static holder - fine for a single-user desktop Swing app like this one.
 */
public final class Session {

    private static User currentUser;

    private Session() { }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
