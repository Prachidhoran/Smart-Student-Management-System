package com.sms.util;

import static org.junit.jupiter.api.Assertions.*;
import com.sms.model.AttendanceSummary;
import com.sms.model.ResultRow;
import com.sms.model.Student;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfGeneratorTest {
    @Test void createsACompletePdfReport() throws Exception {
        Student student = new Student();
        student.setStudentId("CSE-001"); student.setFullName("Ada Lovelace"); student.setDepartment("CSE");
        student.setCourse("B.E."); student.setYear(2); student.setSemester(3); student.setMarks(90); student.setAttendance(80);
        Path report = Files.createTempFile("report-", ".pdf");
        try {
            PdfGenerator.studentReport(student, List.of(new ResultRow(1, 1, "CSE-001", "Java", 90, 100)),
                    new AttendanceSummary(1, "CSE-001", "Ada Lovelace", 5, 4, 80), report.toFile());
            String pdf = Files.readString(report);
            assertTrue(pdf.startsWith("%PDF-1.4"));
            assertTrue(pdf.endsWith("%%EOF"));
        } finally { Files.deleteIfExists(report); }
    }
}
