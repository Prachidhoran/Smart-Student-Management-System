package com.sms.util;

import com.sms.model.ResultRow;
import com.sms.model.Student;
import com.sms.model.AttendanceSummary;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public final class PdfGenerator {
    private PdfGenerator() {}

    // Small dependency-free PDF writer for clean mini-project reports.
    public static void studentReport(Student s, List<ResultRow> results, AttendanceSummary attendance, File file) throws Exception {
        if (s == null || file == null) throw new IllegalArgumentException("Student and destination file are required.");
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 18 Tf 50 760 Td (STUDENT REPORT CARD) Tj ET\n");
        line(content, "Name: " + safe(s.getFullName()), 50, 720, 12);
        line(content, "Student ID: " + safe(s.getStudentId()), 50, 700, 12);
        line(content, "Department: " + safe(s.getDepartment()) + "    Course: " + safe(s.getCourse()), 50, 680, 12);
        line(content, "Year: " + s.getYear() + "    Semester: " + s.getSemester(), 50, 660, 12);
        String attendanceText = attendance == null ? "No attendance records" : attendance.presentDays() + "/" + attendance.totalDays() + " days (" + fmt(attendance.percentage()) + "%)";
        line(content, "Overall marks: " + fmt(s.getMarks()) + "%    Grade: " + s.getGrade(), 50, 640, 12);
        line(content, "Attendance: " + attendanceText, 50, 620, 12);
        line(content, "Subject Results", 50, 600, 14);

        int y = 575;
        for (ResultRow r : results) {
            line(content, r.subject() + " - " + fmt(r.marks()) + "/" + fmt(r.maxMarks())
                    + " (" + fmt(r.percentage()) + "%) - Grade " + r.grade(), 60, y, 11);
            y -= 20;
            if (y < 100) break;
        }

        line(content, "Guardian: " + safe(s.getGuardianName()) + "    Phone: " + safe(s.getGuardianPhone()), 50, 100, 11);
        line(content, "Generated: " + LocalDate.now(), 50, 80, 10);
        writePdf(content.toString(), file);
    }

    public static void idCard(Student s, File file) throws Exception {
        if (s == null || file == null) throw new IllegalArgumentException("Student and destination file are required.");
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 20 Tf 50 760 Td (STUDENT ID CARD) Tj ET\n");
        line(content, safe(s.getFullName()), 50, 720, 16);
        line(content, "ID: " + safe(s.getStudentId()), 50, 695, 12);
        line(content, "Department: " + safe(s.getDepartment()), 50, 675, 11);
        line(content, "Course: " + safe(s.getCourse()), 50, 655, 11);
        line(content, "Year: " + s.getYear() + "    Semester: " + s.getSemester(), 50, 635, 11);
        line(content, "Email: " + safe(s.getEmail()), 50, 605, 10);
        line(content, "Phone: " + safe(s.getPhone()), 50, 585, 10);
        line(content, "Guardian: " + safe(s.getGuardianName()), 50, 565, 10);
        line(content, "Generated: " + LocalDate.now(), 50, 525, 11);
        writePdf(content.toString(), file);
    }

    private static void line(StringBuilder b, String text, int x, int y, int size) {
        String safe = escape(text);
        b.append("BT /F1 ").append(size).append(" Tf ")
                .append(x).append(" ").append(y).append(" Td (")
                .append(safe).append(") Tj ET\n");
    }

    private static String safe(String s) { return s == null || s.isBlank() ? "N/A" : s; }
    private static String fmt(double d) { return String.format("%.1f", d); }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace("\r"," ").replace("\n"," ");
    }

    private static void writePdf(String content, File file) throws Exception {
        byte[] stream = content.getBytes(StandardCharsets.US_ASCII);
        String[] objects = new String[] {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
                "<< /Length " + stream.length + " >>\nstream\n" + content + "endstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        };

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
            long[] offsets = new long[objects.length + 1];

            for (int i=0;i<objects.length;i++) {
                offsets[i+1] = out.getChannel().position();
                out.write((i+1 + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
                out.write(objects[i].getBytes(StandardCharsets.US_ASCII));
                out.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            }

            long xref = out.getChannel().position();
            out.write(("xref\n0 " + (objects.length+1) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
            for (int i=1;i<offsets.length;i++) {
                out.write(String.format("%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.US_ASCII));
            }
            out.write(("trailer\n<< /Size " + (objects.length+1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF")
                    .getBytes(StandardCharsets.US_ASCII));
        }
    }
}
