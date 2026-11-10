package com.sms.dao;

import com.sms.model.AttendanceRow;
import com.sms.model.AttendanceSummary;
import com.sms.util.DatabaseConnection;
import com.sms.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {
    public void save(int studentId, String date, String status) {
        ValidationUtil.validateAttendance(date, status);
        String sql = """
            INSERT INTO attendance(student_db_id,attendance_date,status)
            VALUES(?,?,?)
            ON CONFLICT(student_db_id,attendance_date)
            DO UPDATE SET status=excluded.status
            """;
        try (var c=DatabaseConnection.getConnection(); var ps=c.prepareStatement(sql)) {
            ps.setInt(1, studentId); ps.setString(2, date); ps.setString(3, status);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException("Could not save attendance: "+e.getMessage(), e); }
    }

    public List<AttendanceRow> daily(String date) {
        String sql = """
            SELECT a.id,s.id,s.student_id,s.full_name,a.attendance_date,a.status
            FROM attendance a JOIN students s ON s.id=a.student_db_id
            WHERE a.attendance_date=? ORDER BY s.full_name
            """;
        try (var c=DatabaseConnection.getConnection(); var ps=c.prepareStatement(sql)) {
            ps.setString(1,date);
            try(var rs=ps.executeQuery()){
                List<AttendanceRow> list=new ArrayList<>();
                while(rs.next()) list.add(new AttendanceRow(rs.getInt(1),rs.getInt(2),rs.getString(3),
                        rs.getString(4),rs.getString(5),rs.getString(6)));
                return list;
            }
        } catch(Exception e){ throw new RuntimeException("Could not load attendance.",e); }
    }

    public List<AttendanceSummary> monthly(String yyyyMm) {
        String sql = """
            SELECT s.id,s.student_id,s.full_name,
                   COUNT(a.id) total_days,
                   COALESCE(SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END),0) present_days
            FROM students s
            LEFT JOIN attendance a ON a.student_db_id=s.id AND substr(a.attendance_date,1,7)=?
            GROUP BY s.id ORDER BY s.full_name
            """;
        try(var c=DatabaseConnection.getConnection(); var ps=c.prepareStatement(sql)){
            ps.setString(1,yyyyMm);
            try(var rs=ps.executeQuery()){
                List<AttendanceSummary> list=new ArrayList<>();
                while(rs.next()){
                    int total=rs.getInt(4), present=rs.getInt(5);
                    double pct=total==0?100:(present*100.0/total);
                    list.add(new AttendanceSummary(rs.getInt(1),rs.getString(2),rs.getString(3),total,present,pct));
                }
                return list;
            }
        }catch(Exception e){ throw new RuntimeException("Could not load monthly attendance.",e); }
    }

    public List<AttendanceRow> dailyForStudent(int studentId, String date) {
        String sql = """
            SELECT a.id,s.id,s.student_id,s.full_name,a.attendance_date,a.status
            FROM attendance a JOIN students s ON s.id=a.student_db_id
            WHERE a.student_db_id=? AND a.attendance_date=?
            ORDER BY s.full_name
            """;
        try (var c = DatabaseConnection.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId); ps.setString(2, date);
            try (var rs = ps.executeQuery()) {
                List<AttendanceRow> list = new ArrayList<>();
                while (rs.next()) list.add(new AttendanceRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)));
                return list;
            }
        } catch (Exception e) { throw new RuntimeException("Could not load attendance.", e); }
    }

    public AttendanceSummary summaryForStudent(int studentId) {
        String sql = """
            SELECT s.id,s.student_id,s.full_name,COUNT(a.id),
                   COALESCE(SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END),0)
            FROM students s LEFT JOIN attendance a ON a.student_db_id=s.id
            WHERE s.id=? GROUP BY s.id
            """;
        try (var c = DatabaseConnection.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int total = rs.getInt(4), present = rs.getInt(5);
                return new AttendanceSummary(rs.getInt(1), rs.getString(2), rs.getString(3), total, present, total == 0 ? 0 : present * 100.0 / total);
            }
        } catch (Exception e) { throw new RuntimeException("Could not calculate attendance.", e); }
    }
}
