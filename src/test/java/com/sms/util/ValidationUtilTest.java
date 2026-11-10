package com.sms.util;

import static org.junit.jupiter.api.Assertions.*;
import com.sms.model.ResultRow;
import com.sms.model.Student;
import org.junit.jupiter.api.Test;

class ValidationUtilTest {
    @Test void validatesACompleteStudent() {
        Student student = new Student();
        student.setStudentId("CSE-001"); student.setFullName("Ada Lovelace"); student.setGender("Female");
        student.setEmail("ada@example.com"); student.setPhone("9876543210"); student.setDepartment("CSE");
        student.setCourse("B.E."); student.setYear(2); student.setSemester(3); student.setMarks(92); student.setAttendance(88);
        assertDoesNotThrow(() -> ValidationUtil.validateStudent(student));
    }

    @Test void rejectsInvalidMarksAndCalculatesResultGrade() {
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateResult("Java", 101, 100));
        assertEquals("A+", new ResultRow(1, 1, "CSE-001", "Java", 90, 100).grade());
    }
}
