package com.sms.util;

import static org.junit.jupiter.api.Assertions.*;
import com.sms.model.User;
import org.junit.jupiter.api.Test;

class AccessControlTest {
    @Test void studentIsLimitedToTheirOwnRecord() {
        User student = new User(3, "student", "STUDENT", 12);
        assertFalse(AccessControl.canManageStudents(student));
        assertTrue(AccessControl.canViewStudent(student, 12));
        assertFalse(AccessControl.canViewStudent(student, 13));
    }
}
