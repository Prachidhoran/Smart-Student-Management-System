package com.sms.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppLogger {
    private AppLogger() { }
    public static void error(Class<?> source, String message, Throwable cause) {
        Logger.getLogger(source.getName()).log(Level.SEVERE, message, cause);
    }
}
