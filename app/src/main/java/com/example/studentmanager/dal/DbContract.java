package com.example.studentmanager.dal;

/** Khung chung. Doi schema phai qua PR chung va co migration. */
public final class DbContract {
    private DbContract() {}

    public static final String DATABASE_NAME = "study_schedule.db";
    public static final int DATABASE_VERSION = 1;
    public static final String ADMIN = "ADMIN";
    public static final String STUDENT = "STUDENT";
    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";

    public static final class Accounts {
        private Accounts() {}
        public static final String TABLE = "Account";
        public static final String ID = "id";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String ROLE = "role";
    }
    public static final class Students {
        private Students() {}
        public static final String TABLE = "Student";
        public static final String ID = "id";
        public static final String CODE = "studentCode";
        public static final String FULLNAME = "fullname";
        public static final String EMAIL = "email";
        public static final String PHONE = "phone";
        public static final String ACCOUNT_ID = "accountId";
    }
    public static final class Subjects {
        private Subjects() {}
        public static final String TABLE = "Subject";
        public static final String ID = "id";
        public static final String CODE = "subjectCode";
        public static final String NAME = "subjectName";
        public static final String DESCRIPTION = "description";
        public static final String CREDITS = "credits";
    }
    public static final class Schedules {
        private Schedules() {}
        public static final String TABLE = "StudySchedule";
        public static final String ID = "id";
        public static final String STUDENT_ID = "studentId";
        public static final String SUBJECT_ID = "subjectId";
        public static final String DATE = "studyDate";
        public static final String START = "startTime";
        public static final String END = "endTime";
        public static final String LOCATION = "location";
        public static final String STATUS = "status";
    }
}
