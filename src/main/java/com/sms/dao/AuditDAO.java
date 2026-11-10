package com.sms.dao;

import com.sms.util.DatabaseConnection;
import com.sms.util.AppLogger;

public class AuditDAO {
    public void log(String username, String action, String entity, String id, String details) {
        String sql = "INSERT INTO audit_logs(username,action,entity,entity_id,details) VALUES(?,?,?,?,?)";
        try(var c=DatabaseConnection.getConnection();var ps=c.prepareStatement(sql)){
            ps.setString(1,username); ps.setString(2,action); ps.setString(3,entity);
            ps.setString(4,id); ps.setString(5,details); ps.executeUpdate();
        }catch(Exception e){ AppLogger.error(AuditDAO.class, "Could not write audit log", e); }
    }
}
