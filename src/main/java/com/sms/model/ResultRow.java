package com.sms.model;

public record ResultRow(
        int id,
        int studentDbId,
        String studentId,
        String subject,
        double marks,
        double maxMarks
) {
    public double percentage() {
        return maxMarks <= 0 ? 0 : (marks / maxMarks) * 100.0;
    }

    public String grade() {
        double p = percentage();
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B";
        if (p >= 60) return "C";
        if (p >= 50) return "D";
        return "F";
    }
}
