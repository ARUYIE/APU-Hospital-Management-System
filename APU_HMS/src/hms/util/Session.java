package hms.util;

import hms.role.User;

// gets and retain info on current user
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
