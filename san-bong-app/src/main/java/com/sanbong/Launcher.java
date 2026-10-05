package com.sanbong;

/**
 * Separate entry point (not extending Application) so the shaded fat-jar's
 * Main-Class does not trigger the JavaFX "missing runtime components" check.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
