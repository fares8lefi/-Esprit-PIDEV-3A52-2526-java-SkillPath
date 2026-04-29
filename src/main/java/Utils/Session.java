package Utils;

import Models.User;

public class Session {
    private static User currentUser;
    private static String temporaryBanMessage;

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static void logoutKeepTemporaryBanMessage() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null && !"inactive".equalsIgnoreCase(currentUser.getStatus());
    }

    public static void setTemporaryBanMessage(String message) {
        temporaryBanMessage = message;
    }

    public static String consumeTemporaryBanMessage() {
        String message = temporaryBanMessage;
        temporaryBanMessage = null;
        return message;
    }
}
