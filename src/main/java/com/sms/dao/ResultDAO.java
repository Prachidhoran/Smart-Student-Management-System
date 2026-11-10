package com.sms.dao;

import com.sms.model.ResultRow;
import com.sms.util.DatabaseConnection;
import com.sms.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

public class ResultDAO {
    public void save(int studentId, String subject, double marks, double maxMarks) {
        ValidationUtil.validateResult(subject, marks, maxMarks);
        String sql = """
            INSERT INTO results(student_db_id,subject,marks,max_marks)
            VALUES(?,?,?,?)
            ON CONFLICT(student_db_id,subject)
            DO UPDATE SET marks=excluded.marks,max_marks=excluded.max_marks
            """;
        try(var c=DatabaseConnection.getConnection();var ps=c.prepareStatement(sql)){
            ps.setInt(1,studentId); ps.setString(2,subject); ps.setDouble(3,marks); ps.setDouble(4,maxMarks);
            ps.executeUpdate();
        }catch(Exception e){throw new RuntimeException("Could not save result: "+e.getMessage(),e);}
    }

    public List<ResultRow> byStudent(int studentId) {
        String sql = """
            SELECT r.id,r.student_db_id,s.student_id,r.subject,r.marks,r.max_marks
            FROM results r JOIN students s ON s.id=r.student_db_id
            WHERE r.student_db_id=? ORDER BY r.subject
            """;
        try(var c=DatabaseConnection.getConnection();var ps=c.prepareStatement(sql)){
            ps.setInt(1,studentId);
            try(var rs=ps.executeQuery()){
                List<ResultRow> list=new ArrayList<>();
                while(rs.next()) list.add(new ResultRow(rs.getInt(1),rs.getInt(2),rs.getString(3),
                        rs.getString(4),rs.getDouble(5),rs.getDouble(6)));
                return list;
            }
        }catch(Exception e){throw new RuntimeException("Could not load results.",e);}
    }

    public void delete(int id) {
        try(var c=DatabaseConnection.getConnection();var ps=c.prepareStatement("DELETE FROM results WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        }catch(Exception e){throw new RuntimeException("Could not delete result.",e);}
    }
}
