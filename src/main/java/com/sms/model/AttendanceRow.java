package com.sms.model;

public record AttendanceRow(
        int id,
        int studentDbId,
        String studentId,
        String studentName,
        String date,
        String status
) {}
