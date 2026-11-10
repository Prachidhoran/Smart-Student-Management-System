package com.sms.dao;

import com.sms.model.Student;
import com.sms.util.DatabaseConnection;
import com.sms.util.ValidationUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    public List<Student> findAll() {
        return query("SELECT * FROM students ORDER BY id DESC");
    }

    public List<Student> search(String q, String department) {
        String sql = """
            SELECT * FROM students
            WHERE (LOWER(student_id) LIKE ? OR LOWER(full_name) LIKE ?
                   OR LOWER(email) LIKE ? OR LOWER(course) LIKE ?)
              AND (? = 'All' OR department = ?)
            ORDER BY id DESC
            """;
        String like = "%" + q.trim().toLowerCase() + "%";
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, like);
            ps.setString(5, department);
            ps.setString(6, department);
            return read(ps);
        } catch (Exception e) {
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    public Student byDbId(int dbId) {
        String sql = "SELECT * FROM students WHERE id=?";
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, dbId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? from(rs) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load student.", e);
        }
    }

    public boolean existsStudentId(String studentId, Integer excludeId) {
        String sql = excludeId == null
                ? "SELECT 1 FROM students WHERE student_id=?"
                : "SELECT 1 FROM students WHERE student_id=? AND id<>?";
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            if (excludeId != null) ps.setInt(2, excludeId);
            try (var rs = ps.executeQuery()) { return rs.next(); }
        } catch (Exception e) {
            throw new RuntimeException("Could not validate Student ID.", e);
        }
    }

    public void insert(Student s) {
        ValidationUtil.validateStudent(s);
        String sql = """
            INSERT INTO students(student_id,full_name,gender,dob,email,phone,address,
            department,course,year,semester,marks,attendance,photo_path,guardian_name,guardian_phone)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement(sql)) {
            fill(ps, s);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Could not add student: " + e.getMessage(), e);
        }
    }

    public void update(Student s) {
        ValidationUtil.validateStudent(s);
        String sql = """
            UPDATE students SET student_id=?,full_name=?,gender=?,dob=?,email=?,phone=?,address=?,
            department=?,course=?,year=?,semester=?,marks=?,attendance=?,photo_path=?,guardian_name=?,guardian_phone=?
            WHERE id=?
            """;
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement(sql)) {
            fill(ps, s);
            ps.setInt(17, s.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Could not update student: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement("DELETE FROM students WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Could not delete student: " + e.getMessage(), e);
        }
    }

    public long count() { return scalar("SELECT COUNT(*) FROM students"); }
    public long countLowAttendance() {
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT s.id,
                       CASE WHEN COUNT(a.id)=0 THEN 100.0
                            ELSE (SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END)*100.0/COUNT(a.id))
                       END pct
                FROM students s
                LEFT JOIN attendance a ON a.student_db_id=s.id
                GROUP BY s.id
            ) WHERE pct < 75
            """;
        return scalar(sql);
    }

    public long countGender(String gender) {
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement("SELECT COUNT(*) FROM students WHERE gender=?")) {
            ps.setString(1, gender);
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 0; }
        } catch (Exception e) { throw new RuntimeException("Statistics failed.", e); }
    }

    public double averageMarks() {
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement("SELECT COALESCE(AVG(marks),0) FROM students");
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) { throw new RuntimeException("Statistics failed.", e); }
    }

    public List<Student> recent(int limit) {
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement("SELECT * FROM students ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            return read(ps);
        } catch (Exception e) { throw new RuntimeException("Recent students failed.", e); }
    }

    public void exportCsv(List<Student> list, File file) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write("Student ID,Name,Gender,DOB,Email,Phone,Department,Course,Year,Semester,Marks,Attendance,Grade,Guardian,Guardian Phone");
            w.newLine();
            for (Student s : list) {
                w.write(csv(s.getStudentId())+","+csv(s.getFullName())+","+csv(s.getGender())+","+csv(s.getDob())+
                        ","+csv(s.getEmail())+","+csv(s.getPhone())+","+csv(s.getDepartment())+","+csv(s.getCourse())+
                        ","+s.getYear()+","+s.getSemester()+","+s.getMarks()+","+s.getAttendance()+","+csv(s.getGrade())+
                        ","+csv(s.getGuardianName())+","+csv(s.getGuardianPhone()));
                w.newLine();
            }
        } catch (Exception e) { throw new RuntimeException("CSV export failed.", e); }
    }

    private String csv(String s) { return s == null ? "" : "\"" + s.replace("\"","\"\"") + "\""; }

    private long scalar(String sql) {
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) { throw new RuntimeException("Database query failed.", e); }
    }

    private List<Student> query(String sql) {
        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement(sql)) { return read(ps); }
        catch (Exception e) { throw new RuntimeException("Could not load students: "+e.getMessage(), e); }
    }

    private List<Student> read(PreparedStatement ps) throws SQLException {
        List<Student> list = new ArrayList<>();
        try (var rs = ps.executeQuery()) {
            while (rs.next()) list.add(from(rs));
        }
        return list;
    }

    private Student from(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setStudentId(rs.getString("student_id"));
        s.setFullName(rs.getString("full_name"));
        s.setGender(rs.getString("gender"));
        s.setDob(rs.getString("dob"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setAddress(rs.getString("address"));
        s.setDepartment(rs.getString("department"));
        s.setCourse(rs.getString("course"));
        s.setYear(rs.getInt("year"));
        s.setSemester(rs.getInt("semester"));
        s.setMarks(rs.getDouble("marks"));
        s.setAttendance(rs.getDouble("attendance"));
        s.setPhotoPath(rs.getString("photo_path"));
        s.setGuardianName(rs.getString("guardian_name"));
        s.setGuardianPhone(rs.getString("guardian_phone"));
        return s;
    }

    private void fill(PreparedStatement ps, Student s) throws SQLException {
        ps.setString(1, s.getStudentId());
        ps.setString(2, s.getFullName());
        ps.setString(3, s.getGender());
        ps.setString(4, s.getDob());
        ps.setString(5, s.getEmail());
        ps.setString(6, s.getPhone());
        ps.setString(7, s.getAddress());
        ps.setString(8, s.getDepartment());
        ps.setString(9, s.getCourse());
        ps.setInt(10, s.getYear());
        ps.setInt(11, s.getSemester());
        ps.setDouble(12, s.getMarks());
        ps.setDouble(13, s.getAttendance());
        ps.setString(14, s.getPhotoPath());
        ps.setString(15, s.getGuardianName());
        ps.setString(16, s.getGuardianPhone());
    }
}
