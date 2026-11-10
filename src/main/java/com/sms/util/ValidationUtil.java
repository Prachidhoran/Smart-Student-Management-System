package com.sms.util;

import com.sms.model.Student;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Central validation shared by the UI and persistence boundary. */
public final class ValidationUtil {
    private ValidationUtil() { }

    public static void validateStudent(Student student) {
        require(student != null, "Student details are required.");
        require(matches(student.getStudentId(), "[A-Za-z0-9_-]{3,20}"), "Student ID must contain 3-20 letters, numbers, underscores, or hyphens.");
        require(student.getFullName() != null && student.getFullName().trim().length() >= 2, "Enter a valid full name.");
        require(oneOf(student.getGender(), "Male", "Female", "Other"), "Select a valid gender.");
        require(matches(student.getEmail(), "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "Enter a valid email address.");
        require(matches(student.getPhone(), "[6-9][0-9]{9}"), "Enter a valid 10-digit mobile number.");
        require(!isBlank(student.getDepartment()) && !isBlank(student.getCourse()), "Select a department and course.");
        require(student.getYear() >= 1 && student.getYear() <= 4, "Year must be between 1 and 4.");
        require(student.getSemester() >= 1 && student.getSemester() <= 8, "Semester must be between 1 and 8.");
        require(inRange(student.getMarks(), 0, 100), "Marks must be between 0 and 100.");
        require(inRange(student.getAttendance(), 0, 100), "Attendance must be between 0 and 100.");
        if (!isBlank(student.getGuardianPhone())) require(matches(student.getGuardianPhone(), "[6-9][0-9]{9}"), "Guardian phone must be a valid 10-digit mobile number.");
        if (!isBlank(student.getDob())) {
            try { LocalDate.parse(student.getDob()); }
            catch (DateTimeParseException ex) { throw new IllegalArgumentException("Date of birth must use YYYY-MM-DD."); }
        }
    }

    public static void validateAttendance(String date, String status) {
        try { LocalDate.parse(date); }
        catch (Exception ex) { throw new IllegalArgumentException("Attendance date must use YYYY-MM-DD."); }
        require(oneOf(status, "PRESENT", "ABSENT"), "Attendance status must be PRESENT or ABSENT.");
    }

    public static void validateResult(String subject, double marks, double maxMarks) {
        require(subject != null && subject.trim().matches(".{1,100}"), "Enter a subject of up to 100 characters.");
        require(Double.isFinite(marks) && Double.isFinite(maxMarks) && maxMarks > 0 && marks >= 0 && marks <= maxMarks,
                "Marks must be between zero and the maximum marks.");
    }

    private static boolean inRange(double value, double minimum, double maximum) { return Double.isFinite(value) && value >= minimum && value <= maximum; }
    private static boolean matches(String value, String pattern) { return value != null && value.trim().matches(pattern); }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static boolean oneOf(String value, String... options) { for (String option : options) if (option.equals(value)) return true; return false; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
