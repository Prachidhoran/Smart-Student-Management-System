package com.sms.model;

public record AttendanceSummary(
        int studentDbId,
        String studentId,
        String studentName,
        int totalDays,
        int presentDays,
        double percentage
) {}
