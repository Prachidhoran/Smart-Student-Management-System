package com.sms.dao;

import static org.junit.jupiter.api.Assertions.*;
import com.sms.model.Student;
import com.sms.util.DatabaseInitializer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DaoIntegrationTest {
    private Path database;

    @BeforeEach void setUp() throws Exception {
        database = Files.createTempFile("sms-test-", ".db");
        System.setProperty("sms.db.path", database.toString());
        DatabaseInitializer.initialize();
    }

    @AfterEach void tearDown() throws Exception {
        System.clearProperty("sms.db.path");
        Files.deleteIfExists(database);
    }

    @Test void savesStudentAttendanceAndResultInAnIsolatedDatabase() {
        Student student = new Student();
        student.setStudentId("TST-101"); student.setFullName("Test Student"); student.setGender("Other");
        student.setEmail("test@example.com"); student.setPhone("9876543210"); student.setDepartment("CSE");
        student.setCourse("B.E."); student.setYear(1); student.setSemester(1); student.setMarks(80); student.setAttendance(75);
        StudentDAO students = new StudentDAO(); students.insert(student);
        Student saved = students.search("TST-101", "All").getFirst();
        new AttendanceDAO().save(saved.getId(), "2026-01-15", "PRESENT");
        new ResultDAO().save(saved.getId(), "Java", 95, 100);
        AttendanceDAO attendance = new AttendanceDAO();
        ResultDAO results = new ResultDAO();
        assertEquals(1, attendance.dailyForStudent(saved.getId(), "2026-01-15").size());
        assertEquals("A+", results.byStudent(saved.getId()).getFirst().grade());
        students.delete(saved.getId());
        assertTrue(attendance.dailyForStudent(saved.getId(), "2026-01-15").isEmpty(), "foreign keys should cascade child deletion");
        assertTrue(results.byStudent(saved.getId()).isEmpty(), "foreign keys should cascade child deletion");
    }

    @Test void seedsHashedDemoPasswordsThatAuthenticate() {
        assertNotNull(new UserDAO().authenticate("admin", "admin123"));
        assertNull(new UserDAO().authenticate("admin", "incorrect"));
    }
}
