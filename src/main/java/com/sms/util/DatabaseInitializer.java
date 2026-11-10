package com.sms.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public final class DatabaseInitializer {
    private DatabaseInitializer() {}

    public static void initialize() {
        String users = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('ADMIN','TEACHER','STUDENT')),
                student_db_id INTEGER,
                FOREIGN KEY(student_db_id) REFERENCES students(id) ON DELETE SET NULL
            )
            """;

        String students = """
            CREATE TABLE IF NOT EXISTS students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT NOT NULL UNIQUE,
                full_name TEXT NOT NULL,
                gender TEXT NOT NULL,
                dob TEXT,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                address TEXT,
                department TEXT NOT NULL,
                course TEXT NOT NULL,
                year INTEGER NOT NULL,
                semester INTEGER NOT NULL,
                marks REAL NOT NULL DEFAULT 0,
                attendance REAL NOT NULL DEFAULT 0,
                photo_path TEXT,
                guardian_name TEXT,
                guardian_phone TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String attendance = """
            CREATE TABLE IF NOT EXISTS attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_db_id INTEGER NOT NULL,
                attendance_date TEXT NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('PRESENT','ABSENT')),
                UNIQUE(student_db_id, attendance_date),
                FOREIGN KEY(student_db_id) REFERENCES students(id) ON DELETE CASCADE
            )
            """;

        String results = """
            CREATE TABLE IF NOT EXISTS results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_db_id INTEGER NOT NULL,
                subject TEXT NOT NULL,
                marks REAL NOT NULL,
                max_marks REAL NOT NULL DEFAULT 100,
                UNIQUE(student_db_id, subject),
                FOREIGN KEY(student_db_id) REFERENCES students(id) ON DELETE CASCADE
            )
            """;

        String audit = """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                action TEXT NOT NULL,
                entity TEXT NOT NULL,
                entity_id TEXT,
                details TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement()) {

            // Students first because users has a foreign key to it.
            s.execute(students);
            s.execute(users);
            s.execute(attendance);
            s.execute(results);
            s.execute(audit);
            s.execute("CREATE INDEX IF NOT EXISTS idx_attendance_student_date ON attendance(student_db_id, attendance_date)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(attendance_date)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_results_student_subject ON results(student_db_id, subject)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_students_student_id ON students(student_id)");

            migratePlaintextPasswords(c);

            // Demo records make the dashboard presentable on first launch.
            s.executeUpdate("""
                INSERT OR IGNORE INTO students
                (student_id,full_name,gender,dob,email,phone,address,department,course,year,semester,marks,attendance,guardian_name,guardian_phone)
                VALUES
                ('CSE2026-001','Arsalan Khan','Male','2006-05-12','arsalan@example.com','9876543210',
                 'Malkapur','CSE','B.E.',2,3,88,86,'Imran Khan','9876501234')
                """);

            s.executeUpdate("""
                INSERT OR IGNORE INTO students
                (student_id,full_name,gender,dob,email,phone,address,department,course,year,semester,marks,attendance,guardian_name,guardian_phone)
                VALUES
                ('CSE2026-002','Zoya Shaikh','Female','2006-08-20','zoya@example.com','9876543211',
                 'Malkapur','CSE','B.E.',2,3,93,94,'Sameer Shaikh','9876501235')
                """);

            s.executeUpdate("""
                INSERT OR IGNORE INTO students
                (student_id,full_name,gender,dob,email,phone,address,department,course,year,semester,marks,attendance,guardian_name,guardian_phone)
                VALUES
                ('ECE2026-001','Ahmed Khan','Male','2005-11-06','ahmed@example.com','9876543212',
                 'Buldhana','ECE','B.E.',2,3,81,72,'Rashid Khan','9876501236')
                """);

            seedUser(c, "admin", "admin123", "ADMIN", null);
            seedUser(c, "teacher", "teacher123", "TEACHER", null);
            try (PreparedStatement findStudent = c.prepareStatement("SELECT id FROM students WHERE student_id=?")) {
                findStudent.setString(1, "CSE2026-001");
                try (ResultSet rs = findStudent.executeQuery()) {
                    if (rs.next()) seedUser(c, "student", "student123", "STUDENT", rs.getInt(1));
                }
            }

            // Seed results for demo student profiles.
            s.executeUpdate("""
                INSERT OR IGNORE INTO results(student_db_id,subject,marks,max_marks)
                SELECT id,'Java',92,100 FROM students WHERE student_id='CSE2026-001'
                """);
            s.executeUpdate("""
                INSERT OR IGNORE INTO results(student_db_id,subject,marks,max_marks)
                SELECT id,'Data Structures',86,100 FROM students WHERE student_id='CSE2026-001'
                """);
            s.executeUpdate("""
                INSERT OR IGNORE INTO results(student_db_id,subject,marks,max_marks)
                SELECT id,'DBMS',89,100 FROM students WHERE student_id='CSE2026-001'
                """);

            // Seed some attendance for alert/demo behaviour.
            s.executeUpdate("""
                INSERT OR IGNORE INTO attendance(student_db_id,attendance_date,status)
                SELECT id,date('now','-5 day'),'PRESENT' FROM students WHERE student_id='CSE2026-001'
                """);
            s.executeUpdate("""
                INSERT OR IGNORE INTO attendance(student_db_id,attendance_date,status)
                SELECT id,date('now','-4 day'),'PRESENT' FROM students WHERE student_id='CSE2026-001'
                """);
            s.executeUpdate("""
                INSERT OR IGNORE INTO attendance(student_db_id,attendance_date,status)
                SELECT id,date('now','-3 day'),'ABSENT' FROM students WHERE student_id='CSE2026-001'
                """);
            s.executeUpdate("""
                INSERT OR IGNORE INTO attendance(student_db_id,attendance_date,status)
                SELECT id,date('now','-2 day'),'PRESENT' FROM students WHERE student_id='CSE2026-001'
                """);
            s.executeUpdate("""
                INSERT OR IGNORE INTO attendance(student_db_id,attendance_date,status)
                SELECT id,date('now','-1 day'),'PRESENT' FROM students WHERE student_id='CSE2026-001'
                """);
        } catch (Exception e) {
            AppLogger.error(DatabaseInitializer.class, "Database initialization failed", e);
            throw new IllegalStateException("The application database could not be initialized. Check the application log.", e);
        }
    }

    /** Migrates legacy demo databases without requiring the user to delete data. */
    private static void migratePlaintextPasswords(Connection connection) throws Exception {
        try (PreparedStatement select = connection.prepareStatement("SELECT id, password FROM users");
             ResultSet users = select.executeQuery();
             PreparedStatement update = connection.prepareStatement("UPDATE users SET password=? WHERE id=?")) {
            while (users.next()) {
                String stored = users.getString("password");
                if (!PasswordUtil.isHash(stored)) {
                    update.setString(1, PasswordUtil.hash(stored.toCharArray()));
                    update.setInt(2, users.getInt("id"));
                    update.addBatch();
                }
            }
            update.executeBatch();
        }
    }

    private static void seedUser(Connection connection, String username, String password, String role, Integer studentDbId) throws Exception {
        try (PreparedStatement check = connection.prepareStatement("SELECT 1 FROM users WHERE username=?")) {
            check.setString(1, username);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO users(username,password,role,student_db_id) VALUES(?,?,?,?)")) {
            insert.setString(1, username);
            insert.setString(2, PasswordUtil.hash(password.toCharArray()));
            insert.setString(3, role);
            if (studentDbId == null) insert.setNull(4, java.sql.Types.INTEGER); else insert.setInt(4, studentDbId);
            insert.executeUpdate();
        }
    }
}
