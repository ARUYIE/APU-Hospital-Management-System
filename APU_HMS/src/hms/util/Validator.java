package hms.util;

import java.util.regex.Pattern;

//To validate the login and register of users

public final class Validator {

    // put to false to enable this validator file
    private static final boolean TEST_MODE = true;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private Validator() { }

    // check if its not empty
    public static boolean isNonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // check if its a valid email format
    public static boolean isValidEmail(String email) {
        if (TEST_MODE) return isNonEmpty(email);
        return isNonEmpty(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // accepts digits, spaces, +, - only, and requires at least 7 characters.
    public static boolean isValidPhone(String phone) {
        if (TEST_MODE) return isNonEmpty(phone);
        return isNonEmpty(phone) && phone.trim().matches("^[0-9+\\-\\s]{7,}$");
    }

    // positive number
    public static boolean isPositiveNumber(String value) {
        if (TEST_MODE) return isNonEmpty(value);
        try {
            return Double.parseDouble(value.trim()) > 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    public static boolean isNonNegativeInteger(String value) {
        if (TEST_MODE) return isNonEmpty(value);
        try {
            return Integer.parseInt(value.trim()) >= 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    // password must be at least 6 characters
    public static boolean isValidPassword(String password) {
        if (TEST_MODE) return isNonEmpty(password);
        return password != null && password.length() >= 6;
    }
}