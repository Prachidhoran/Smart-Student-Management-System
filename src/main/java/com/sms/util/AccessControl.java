package com.sms.util;

import com.sms.model.User;

/** Keeps role checks consistent even when a view is opened directly. */
public final class AccessControl {
    private AccessControl() { }
    public static boolean canManageStudents(User user) { return hasRole(user, "ADMIN") || hasRole(user, "TEACHER"); }
    public static boolean canManageAcademicRecords(User user) { return canManageStudents(user); }
    public static boolean canViewStudent(User user, int studentDbId) { return canManageStudents(user) || (hasRole(user, "STUDENT") && user.getStudentId() != null && user.getStudentId() == studentDbId); }
    public static boolean isStudent(User user) { return hasRole(user, "STUDENT"); }
    private static boolean hasRole(User user, String role) { return user != null && role.equals(user.getRole()); }
}
